package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.common.{given, *}
import com.kabasoft.iws.domain.{FinancialsTransaction, FinancialsTransactionDetails, Journal, PeriodicAccountBalance, TPeriodicAccountBalance, common}
import com.kabasoft.iws.repository.{FinancialsTransactionRepository, JournalRepository, PacRepository, PostFinancialsTransactionRepository}
import zio._
import zio.prelude.FlipOps

final class FinancialsServiceLive(pacRepo: PacRepository
                                  , ftrRepo: FinancialsTransactionRepository
                                  , journalRepo: JournalRepository
                                  , repository4PostingTransaction:PostFinancialsTransactionRepository)
             extends FinancialsService:

  override def journal(accountId: String, fromPeriod: Int, toPeriod: Int, company: String): ZIO[Any, RepositoryError, List[Journal]] =
    for {
      queries <- journalRepo.find4Period(accountId, fromPeriod, toPeriod, company).map(_.toList)
    } yield queries

  def getBy(id: String, company: String): ZIO[Any, RepositoryError, PeriodicAccountBalance] =
    pacRepo.getById(id, PeriodicAccountBalance.MODELID, company)

  def getByIds(ids: List[String], company: String): ZIO[Any, RepositoryError, List[PeriodicAccountBalance]] =
    pacRepo.getBy(ids, PeriodicAccountBalance.MODELID, company)

  override def postTransaction4Period(fromPeriod: Int, toPeriod: Int, modelid: Int, company: String): ZIO[Any, RepositoryError, Int] =
    for {
      models <- ftrRepo.find4Period(fromPeriod, toPeriod, modelid, company, false)
      nr     <- ZIO.foreach(models)(trans => postTransaction(trans, company)).map(_.toList).map(_.sum)
    } yield nr

  override def postAll(ids: List[Long], company: String): ZIO[Any, RepositoryError, Int] =
    for {
      queries <- ZIO.foreach(ids)(id => ftrRepo.getByTransId((id, company)))
      models = queries.filter(_.posted == false)
      _ <-ZIO.foreachDiscard(models.map(_.id)) (
        id =>ZIO.logDebug(s"Posting transaction with id ${id} of company ${company}"))
      nr <- ZIO.foreach(models)(model => postTransaction(model, company)).map(_.sum)
    } yield nr

  override def post(id: Long, company: String): ZIO[Any, RepositoryError, Int] =
    ZIO.logDebug(s" Posting transaction with id ${id} of company ${company}") *>
      ftrRepo
        .getByTransId((id, company))
        .flatMap(trans => postTransaction(trans, company))

  private def postTransaction(transaction: FinancialsTransaction, company: String): ZIO[Any, RepositoryError, Int] = {
    val model = transaction.copy(period = common.getPeriod(transaction.transdate))
    val pacids = buildPacIds(model)
    for {

      pacs <- pacRepo.getBy(pacids, PeriodicAccountBalance.MODELID, company).map(_.filterNot(_.id.equals(PeriodicAccountBalance.dummy.id)))
      newRecords = PeriodicAccountBalance.create(model).filterNot(pac => pacs.map(_.id).contains(pac.id))
        .groupBy(_.id) map { case (_, v) => common.reduce(v, PeriodicAccountBalance.dummy)}
      tpacs <- pacs.map(TPeriodicAccountBalance.apply).flip
      oldPacs <- updatePac(model, tpacs).map(e=>e.map(PeriodicAccountBalance.applyT))
      journalEntries <- makeJournal(model, newRecords.toList, oldPacs)
      post <- repository4PostingTransaction.post(List(model), newRecords.toList, oldPacs.flip, journalEntries)

    } yield post

  }

  private def buildPacId(period: Int, accountId: String): String =
    PeriodicAccountBalance.createId(period, accountId)

  private def buildPacIds(model: FinancialsTransaction): List[String] = {
    val pacIds: List[String] = model.lines.map(line => buildPacId(model.getPeriod, line.account))
    val pacOids: List[String] = model.lines.map(line => buildPacId(model.getPeriod, line.oaccount))
    (pacIds ++ pacOids).distinct
  }

  private def updatePac(model: FinancialsTransaction, tpacs: List[TPeriodicAccountBalance]): ZIO[Any, Nothing, List[TPeriodicAccountBalance]] = {
  val result = for{
    newRecords<- groupById(PeriodicAccountBalance.create(model)).map(TPeriodicAccountBalance.apply).flip
             _<- newRecords.map ( pac =>transfer(pac, tpacs)).flip
  }yield ( if(tpacs.nonEmpty) tpacs  else newRecords)
  result
}

  private def transfer( pac:TPeriodicAccountBalance,  tpacs:List[TPeriodicAccountBalance]): ZIO[Any, Nothing, Option[Unit]] =
    tpacs.find(_.id ==  pac.id).map(pac_ => pac_.transfer(pac, pac_)).flip

  private def groupById(r: List[PeriodicAccountBalance]) = {
   val r0 = (r.groupBy(_.id) map { case (_, v) =>
      common.reduce(v, PeriodicAccountBalance.dummy)
    }).toList
    r0
  }



  def findOrBuildPac(pacId: String, period_ : Int, pacList: List[PeriodicAccountBalance]): PeriodicAccountBalance =
    pacList.find(pac_ => pac_.id == pacId).getOrElse(PeriodicAccountBalance.dummy.copy(id = pacId, period = period_))


  private def makeJournal(model: FinancialsTransaction,  pacListx: List[PeriodicAccountBalance], tpacList: List[UIO[PeriodicAccountBalance]]): ZIO[Any, Nothing, List[Journal]] = for {
    pacList<-tpacList.flip
    journal = model.lines.flatMap { line =>
      val pacId = buildPacId(model.getPeriod, line.account)
      val poacId = buildPacId(model.getPeriod, line.oaccount)
      val pac = findOrBuildPac(pacId, model.getPeriod, pacList++pacListx)
      val poac = findOrBuildPac(poacId, model.getPeriod, pacList++pacListx)
      val jou1 = buildJournalEntries(model, line, pac, line.account, line.oaccount, line.side)
      val jou2 = buildJournalEntries(model, line, poac, line.oaccount, line.account, !line.side)
      List(jou1, jou2)
    }
  } yield journal
  private def buildJournalEntries(model: FinancialsTransaction, line: FinancialsTransactionDetails,
                                  pac: PeriodicAccountBalance, account: String, oaccount: String,side:Boolean) =
    Journal(
      -1,
      model.id,
      model.oid,
      account,
      oaccount,
      model.transdate,
      model.postingdate,
      model.enterdate,
      model.getPeriod,
      line.amount,
      pac.idebit,
      pac.debit,
      pac.icredit,
      pac.credit,
      line.currency,
      side,
      line.text,
      model.month.toInt,
      model.year,
      model.company,
      model.modelid
    )


object FinancialsServiceLive:
  val live: ZLayer[PacRepository& FinancialsTransactionRepository& JournalRepository& PostFinancialsTransactionRepository
    , RepositoryError, FinancialsService] =
    ZLayer.fromFunction(new FinancialsServiceLive(_, _, _, _))
