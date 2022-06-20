package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{
  DerivedTransaction,
  FinancialsTransaction,
  FinancialsTransactionDetails,
  Journal,
  PeriodicAccountBalance
}
import com.kabasoft.iws.repository.{ JournalRepository, PacRepository, TransactionRepository }

import java.time.Instant
import zio._

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
      all     <- ZIO.foreach(models)(debitOrCreditPACAll(_, company))
    } yield all

  def getBy(id: String, company: String): ZIO[Any, RepositoryError, PeriodicAccountBalance] =
    pacRepo.getBy(id, company)

  def getByIds(ids: List[String], company: String): ZIO[Any, RepositoryError, List[PeriodicAccountBalance]] =
    pacRepo.getByIds(ids, company)

  override def postTransaction4Period(
    fromPeriod: Int,
    toPeriod: Int,
    company: String
  ): ZIO[Any, RepositoryError, List[Int]] =
    for {
      queries <- ftrRepo.find4Period(fromPeriod, toPeriod, company).runCollect
      models   = FinancialsTransaction.applyD(queries.toList.map(DerivedTransaction.unapply(_).get))
      all     <- ZIO.foreach(models)(trans => debitOrCreditPACAll(trans, company))
    } yield all

  override def post(model: DerivedTransaction, company: String): ZIO[Any, RepositoryError, List[Int]] =
    postAll(List(model.id), company)

  private[this] def debitOrCreditPACAll(model: FinancialsTransaction, company: String): ZIO[Any, RepositoryError, Int] =
    for {
      // pac           <- ZIO.foreach(getIds(model))(pacRepo.getBy(_, company))
      pacs          <- pacRepo.getByIds(getIds(model), company)
      newRecords     = PeriodicAccountBalance.create(model).filterNot(pacs.toSet).distinct
      journalEntries = model.lines.flatMap(line => createJournalEntries(line, model, pacs ::: newRecords))
      pac_created   <- pacRepo.create(newRecords)
      pac_updated   <- pacRepo.modify(pacs)
      model_         = model.copy(posted = true).copy(postingdate = Instant.now())
      trans_posted  <- ftrRepo.modify(model_)
      journal       <- journalRepo.create(journalEntries)
    } yield pac_created + pac_updated + journal + trans_posted

  private[this] def getIds(model: FinancialsTransaction): List[String] = {
    val ids: List[String]  = model.lines.map(line => PeriodicAccountBalance.createId(model.getPeriod, line.account))
    val oids: List[String] = model.lines.map(line => PeriodicAccountBalance.createId(model.getPeriod, line.oaccount))
    (ids ++ oids).distinct
  }

  private def makeJournal(line: FTDetails, model: FinancialsTransaction, pac: DPAC, account: String, oaccount: String) =
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

  private[this] def createJournalEntries(
    line: FTDetails,
    model: FinancialsTransaction,
    pacList: List[DPAC]
  ): List[Journal] = {

    val pacId      = PeriodicAccountBalance.createId(model.getPeriod, line.account)
    val poacId     = PeriodicAccountBalance.createId(model.getPeriod, line.oaccount)
    val dummyAcc   = PeriodicAccountBalance.dummy
    val pac: DPAC  = pacList.find(pac_ => pac_.id == pacId).getOrElse(dummyAcc)
    val poac: DPAC = pacList.find(poac_ => poac_.id == poacId).getOrElse(dummyAcc)
    val jou1       = makeJournal(line, model, pac, line.account, line.oaccount)
    val jou2       = makeJournal(line, model, poac, line.oaccount, line.account)
    List(jou1, jou2)
  }

}

object FinancialsServiceImpl {
  val live
    : ZLayer[PacRepository with TransactionRepository with JournalRepository, RepositoryError, FinancialsService] =
    ZLayer.fromFunction(new FinancialsServiceImpl(_, _, _))
}
