package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.common._
import com.kabasoft.iws.domain.{DerivedTransaction, FinancialsTransaction, FinancialsTransactionDetails, Journal, PeriodicAccountBalance, TPeriodicAccountBalance, common}
import com.kabasoft.iws.repository.{JournalRepository, PacRepository, TransactionRepository}

import java.time.Instant
import zio._
import zio.prelude.FlipOps

final class FinancialsServiceImpl(
                                   pacRepo: PacRepository,
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
    val model_         = model.copy(posted = true, postingdate = Instant.now())
    for {
      pacs          <- pacRepo.getByIds(buildPacIds(model), company)
      tpacs          <-  ZIO.collectAll(pacs.map(TPeriodicAccountBalance.apply(_)))
      xx:Iterable[PeriodicAccountBalance] = PeriodicAccountBalance.create(model).filterNot(pacs.contains).groupBy(_.id) map {
        case (k, v) =>  common.reduce(v, PeriodicAccountBalance.dummy)//.copy(id = k)
      }//.toList
      newRecords     <-  ZIO.collectAll(xx.map(TPeriodicAccountBalance.apply(_)))
      oldPacs        <-   updatePac(model, tpacs)
      newPacs        =   newRecords.map(PeriodicAccountBalance.applyT(_)).flip
      oldtPacs       =  oldPacs.map(pacs=>pacs.map(PeriodicAccountBalance.applyT(_)).flip)
      //oldtPacs       = oldPacs.map(pacs=>pacs.map(PeriodicAccountBalance.applyT(_))).flip
      pac_created   <- newPacs.map(x=> pacRepo.create(x.toList))
      //pac_created1   <- pac_created.
      pac_updated   <- persistPac(oldtPacs.flip)
      //pac_updated   <- oldtPacs.map(pacRepo.modify)

      trans_posted  <- ftrRepo.modify(model_)
      //journal       <- journalRepo.create(journalEntries)
    //} yield pac_created + pac_updated + journal + trans_posted
    } yield   trans_posted

  }

  private[this] def buildPacId(period:Int, accountId:String):String =
    PeriodicAccountBalance.createId(period, accountId)
  private[this] def buildPacIds(model: FinancialsTransaction): List[String] = {
    val pacIds: List[String]  = model.lines.map(line => buildPacId(model.getPeriod, line.account))
    val pacOids: List[String] = model.lines.map(line => buildPacId(model.getPeriod, line.oaccount))
    (pacIds ++ pacOids).distinct
  }

  private def persistPac(pacs: ZIO[Any, Nothing, List[List[PeriodicAccountBalance]]])= for {
    x <- pacs.map(x => pacRepo.modify(x.flatten))
  } yield x



  private def updatePac(model: FinancialsTransaction, tpacs: List[DPAC]) =for {
     x<- ZIO.foreach(model.lines) { line =>
       val pacs = tpacs.filter(pac_ => pac_.id == buildPacId(model.getPeriod, line.account))//.map(_.debiting(line.amount))
       val poacs = tpacs.filter(poac_ => poac_.id == buildPacId(model.getPeriod, line.oaccount))//.map(_.crediting(line.amount))
       TPeriodicAccountBalance.debitAndCreditAll(pacs, poacs,line.amount)
       ZIO.succeed(List(pacs, poacs))
       }
  }yield x.flatten

  /*private def makeJournal(model: FinancialsTransaction,  pacList: List[DPAC]):List[Journal]=for {
    journal <- model.lines.flatMap { line =>
      val pac: Option[DPAC] = pacList.find(pac_ => pac_.id == buildPacId(model.getPeriod, line.account))
      val poac: Option[DPAC] = pacList.find(poac_ => poac_.id == buildPacId(model.getPeriod, line.oaccount))
      val jou1 = pac.map(buildJournalEntries(model, line, _, line.account, line.oaccount))
      val jou2 = poac.map(buildJournalEntries(model, line, _, line.oaccount, line.account))
      List(jou1, jou2).flatten
    }
  }yield journal.flatten

  def buildJournalEntries(model: FinancialsTransaction, line: FTDetails, tpac: DPAC, account:String, oaccount:String)=for{
    idebit <- tpac.debit.get.commit
    icredit <- tpac.debit.get.commit
    debit <- tpac.debit.get.commit
    credit <- tpac.debit.get.commit
  } yield
    Journal(
      -1,
      model.tid,
      model.oid,
      account,
      oaccount,
      model.transdate,
      model.postingdate,
      model.enterdate,
      model.getPeriod,
      line.amount,
      idebit,
      debit,
      icredit,
      credit,
      line.currency,
      line.side,
      line.text,
      model.month.toInt,
      model.year,
      model.company,
      // model.typeJournal,
      model.file_content,
      model.modelid
    )
 */
}



object FinancialsServiceImpl {
  val live: ZLayer[PacRepository with TransactionRepository with JournalRepository, RepositoryError, FinancialsService] =
    ZLayer.fromFunction(new FinancialsServiceImpl(_, _, _))
}
