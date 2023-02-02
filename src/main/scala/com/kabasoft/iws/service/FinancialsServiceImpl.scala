package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.common._
import com.kabasoft.iws.domain.{DerivedTransaction, FinancialsTransaction, FinancialsTransactionDetails, Journal, PeriodicAccountBalance, TPeriodicAccountBalance, common}
import com.kabasoft.iws.repository.{JournalRepository, PacRepository, TransactionRepository}
import zio._
import zio.prelude.FlipOps

import java.time.Instant

final class FinancialsServiceImpl(pacRepo: PacRepository,
                                   ftrRepo: TransactionRepository,
                                   journalRepo: JournalRepository
                                 ) extends FinancialsService {
  type DPAC      = TPeriodicAccountBalance
  type FTDetails = FinancialsTransactionDetails

  override def create(model: FinancialsTransaction): ZIO[Any, RepositoryError, Int] =
    ftrRepo.create(model)

  override def create(item: DerivedTransaction): ZIO[Any, RepositoryError, Int] =
    ftrRepo.create(item)

  override def create(items: List[DerivedTransaction]): ZIO[Any, RepositoryError, Int] =
    ftrRepo.create(items)

  override def postAll(ids: List[Long], company: String): ZIO[Any, RepositoryError, List[Int]] =
    for {
      queries <- ZIO.foreach(ids)(id => ftrRepo.getByTransId(id, company))
      models   = queries.filter(_.posted == false)
      all     <- ZIO.foreach(models)(postTransaction(_, company))
    } yield all

  override def journal(accountId: String, fromPeriod: Int, toPeriod: Int, company: String): ZIO[Any, RepositoryError, List[Journal]] =
    for {
      queries <- journalRepo.find4Period(accountId, fromPeriod, toPeriod, company).runCollect.map(_.toList)
    } yield queries

  def getBy(id: String, company: String): ZIO[Any, RepositoryError, PeriodicAccountBalance] =
    pacRepo.getBy(id, company)

  def getByIds(ids: List[String], company: String): ZIO[Any, RepositoryError, List[PeriodicAccountBalance]] =
    pacRepo.getByIds(ids, company)

  override def postTransaction4Period(fromPeriod: Int, toPeriod: Int, company: String
                                     ): ZIO[Any, RepositoryError, List[Int]] =
    for {
      models <- ftrRepo.find4Period(fromPeriod, toPeriod, company)
        .filter(_.posted == false)
        .runCollect
      nr     <- ZIO.foreach(models)(trans => postTransaction(trans, company)).map(_.toList)
    } yield nr

  override def post(id: Long, company: String): ZIO[Any, RepositoryError, Int] =
    ftrRepo.getByTransId(id, company)
      .flatMap(trans =>postTransaction(trans, company))

  private[this] def postTransaction(transaction: FinancialsTransaction, company: String): ZIO[Any, RepositoryError, Int] = {
    val model = transaction.copy(period = common.getPeriod(transaction.transdate))
    for {
      pacs <- pacRepo.getByIds(buildPacIds(model), company)
      tpacs <- ZIO.foreach(pacs)(TPeriodicAccountBalance.apply)
      xx = PeriodicAccountBalance.create(model).filterNot(pacs.contains).groupBy(_.id) map {
        case (_, v) => common.reduce(v, PeriodicAccountBalance.dummy) //.copy(id = k)
      }
      newRecords <- ZIO.foreach(xx.toList)(TPeriodicAccountBalance.apply)
      newPacs <- updatePac(model, newRecords)
      oldPacs <- updatePac(model, tpacs)
      newTPacs = newPacs.map(_.map(PeriodicAccountBalance.applyT).flip)
      oldTPacs = oldPacs.map(_.map(PeriodicAccountBalance.applyT).flip)
      j<-flattenAndConcat(newTPacs.flip, oldTPacs.flip )
      journalEntries<-  ZIO.succeed(makeJournal(model,j))
      pac_created <- newTPacs.flip.flatMap(x => pacRepo.create(x.flatten))
      _<-ZIO.logDebug(s" number of  pacs created  posted ${pac_created}")
      pac_updated <- oldTPacs.flip.flatMap(x => pacRepo.modify(x.flatten))
      _<-ZIO.logDebug(s" number of  pacs updated  posted ${pac_updated}")
      trans_posted <- ftrRepo.modify(model.copy(posted = true, postingdate = Instant.now()))
      _<-ZIO.logDebug(s" number of  transaction posted ${trans_posted}")
      journals<- journalRepo.create(journalEntries)
      _<-ZIO.logDebug(s"Created  ${journals} journal entries")
    } yield trans_posted+pac_created+pac_updated+journals
  }



  private[this] def buildPacId(period: Int, accountId: String): String =
    PeriodicAccountBalance.createId(period, accountId)

  private[this] def buildPacIds(model: FinancialsTransaction): List[String] = {
    val pacIds: List[String] = model.lines.map(line => buildPacId(model.getPeriod, line.account))
    val pacOids: List[String] = model.lines.map(line => buildPacId(model.getPeriod, line.oaccount))
    (pacIds ++ pacOids).distinct
  }

  private def flattenAndConcat(newPacs: UIO[List[List[PeriodicAccountBalance]]], oldPacs: UIO[List[List[PeriodicAccountBalance]]]) = for {
    newtPacs <-newPacs.map(_.flatten)
    oldtPacs <-oldPacs.map(_.flatten)
  }yield newtPacs++oldtPacs




  private def updatePac(model: FinancialsTransaction, tpacs: List[DPAC]) = for {
    x <- ZIO.foreach(model.lines) { line =>
      val pacs = tpacs.filter(pac_ => pac_.id == buildPacId(model.getPeriod, line.account))
      val poacs = tpacs.filter(poac_ => poac_.id == buildPacId(model.getPeriod, line.oaccount))
      TPeriodicAccountBalance.debitAndCreditAll(pacs, poacs, line.amount)
      ZIO.succeed(List(pacs, poacs))
    }
  } yield x.flatten

  private def makeJournal(model: FinancialsTransaction,  pacList: List[PeriodicAccountBalance]):List[Journal]=for {
    journal <- model.lines.flatMap { line =>
      val pac: Option[PeriodicAccountBalance] = pacList.find(pac_ => pac_.id == buildPacId(model.getPeriod, line.account))
      val poac: Option[PeriodicAccountBalance] = pacList.find(poac_ => poac_.id == buildPacId(model.getPeriod, line.oaccount))
      val jou1 = pac.map(buildJournalEntries(model, line, _, line.account, line.oaccount))
      val jou2 = poac.map(buildJournalEntries(model, line, _, line.oaccount, line.account))
      List(jou1, jou2).flatten
    }
  }yield journal

  def buildJournalEntries(model: FinancialsTransaction, line: FTDetails, pac: PeriodicAccountBalance, account:String, oaccount:String)=
    Journal(
      -1,
      model.id,
      //model.oid,
      account,
      oaccount,
      model.transdate,
      //model.postingdate,
      //model.enterdate,
      model.getPeriod,
      line.amount,
      pac.idebit,
      pac.debit,
      pac.icredit,
      pac.credit,
      line.currency,
      line.side,
      line.text,
      model.month.toInt,
      model.year,
      model.company,
      //model.file_content,
      model.modelid
    )
}



object FinancialsServiceImpl {
  val live: ZLayer[PacRepository with TransactionRepository with JournalRepository, RepositoryError, FinancialsService] =
    ZLayer.fromFunction(new FinancialsServiceImpl(_, _, _))
}
