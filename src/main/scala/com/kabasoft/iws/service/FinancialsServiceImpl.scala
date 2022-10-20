package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.common.reduce
import com.kabasoft.iws.domain.{DerivedTransaction, FinancialsTransaction, FinancialsTransactionDetails, Journal, PeriodicAccountBalance, common}
import com.kabasoft.iws.repository.{JournalRepository, PacRepository, TransactionRepository}

import java.time.Instant
import zio._
import scala.List

final class FinancialsServiceImpl(
  pacRepo: PacRepository,
  ftrRepo: TransactionRepository,
  journalRepo: JournalRepository
) extends FinancialsService {
  type DPAC      = PeriodicAccountBalance
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
      pacs          <- pacRepo.getByIds(buildPacIds(model), company)
      newRecords     = PeriodicAccountBalance.create(model).filterNot(pacs.contains).distinct
      oldPacs        = updatePac(model, pacs)
      journalEntries = makeJournal(model, oldPacs ::: newRecords)
      pac_created   <- pacRepo.create(newRecords)
      pac_updated   <- pacRepo.modify(oldPacs)
      model_         = model.copy(posted = true, postingdate = Instant.now())
      trans_posted  <- ftrRepo.modify(model_)
      journal       <- journalRepo.create(journalEntries)
    } yield pac_created + pac_updated + journal + trans_posted
  }

  private[this] def buildPacId(model: FinancialsTransaction, accountId:String):String =
    PeriodicAccountBalance.createId(model.getPeriod, accountId)
  private[this] def buildPacIds(model: FinancialsTransaction): List[String] = {
    val pacIds: List[String]  = model.lines.map(line => buildPacId(model, line.account))
    val pacOids: List[String] = model.lines.map(line => buildPacId(model, line.oaccount))
    (pacIds ++ pacOids).distinct
  }

  private def updatePac(model: FinancialsTransaction, pacList: List[DPAC]) = {
    val dummyPac = PeriodicAccountBalance.dummy
    model.lines.flatMap { line =>
      val pac    = pacList.find(pac_ => pac_.id == buildPacId(model, line.account)).map(_.debiting(line.amount))
      val poac   = pacList.find(poac_ => poac_.id == buildPacId(model, line.oaccount)).map(_.crediting(line.amount))
      (pac ++ poac)
    }.groupBy(_.id)
      .map { case (_, v) => reduce(v, dummyPac) }
      .filterNot(_.id == dummyPac.id)
      .toList
      .distinct

  }

  private def makeJournal(model: FinancialsTransaction,  pacList: List[DPAC]):List[Journal]=
    model.lines.flatMap { line =>
      val pac: Option[DPAC] = pacList.find(pac_ => pac_.id == buildPacId(model, line.account))
      val poac: Option[DPAC] = pacList.find(poac_ => poac_.id == buildPacId(model, line.oaccount))
      val jou1: Option[Journal]  = pac.map(buildJournalEntries(model, line, _, line.account, line.oaccount))
      val jou2: Option[Journal]  = poac.map(buildJournalEntries(model, line, _, line.oaccount, line.account))
      List(jou1, jou2)
    }.flatten
    def buildJournalEntries(model: FinancialsTransaction, line: FTDetails, pac: DPAC, account:String, oaccount:String)=
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
      // model.typeJournal,
      model.file_content,
      model.modelid
    )

}

object FinancialsServiceImpl {
  val live: ZLayer[PacRepository with TransactionRepository with JournalRepository, RepositoryError, FinancialsService] =
    ZLayer.fromFunction(new FinancialsServiceImpl(_, _, _))
}
