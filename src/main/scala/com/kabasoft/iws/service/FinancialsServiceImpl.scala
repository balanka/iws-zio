package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.common.reduce
import com.kabasoft.iws.domain.{ common, DerivedTransaction, FinancialsTransaction, FinancialsTransactionDetails, Journal, PeriodicAccountBalance }
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
      all     <- ZIO.foreach(models)(postTransaction(_, company))
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
      all     <- ZIO.foreach(models)(trans => postTransaction(trans, company))
    } yield all

  // override def post(model: DerivedTransaction, company: String): ZIO[Any, RepositoryError, List[Int]] =
  //   postAll(List(model.id), company)

  // override def post(model: FinancialsTransaction, company: String): ZIO[Any, RepositoryError, Int] =
  //   postTransaction(model, company)

  override def post(id: Long, company: String): ZIO[Any, RepositoryError, Int] =
    for {
      trans <- ftrRepo.getByTransId(id, company)
      nr    <- postTransaction(trans, company)
    } yield nr

  private[this] def postTransaction(transaction: FinancialsTransaction, company: String): ZIO[Any, RepositoryError, Int] = {
    val model = transaction.copy(period = common.getPeriod(transaction.transdate))
    for {
      pacs          <- pacRepo.getByIds(getIds(model), company)
      newRecords     = PeriodicAccountBalance.create(model).filterNot(pacs.contains).distinct
      oldPacs        = updatePac(model, pacs)
      journalEntries = createJournalEntries(model, oldPacs ::: newRecords)
      pac_created   <- pacRepo.create(newRecords)
      pac_updated   <- pacRepo.modify(oldPacs)
      model_         = model.copy(posted = true, postingdate = Instant.now())
      trans_posted  <- ftrRepo.modify(model_)
      journal       <- journalRepo.create(journalEntries)
    } yield pac_created + pac_updated + journal + trans_posted
  }

  private[this] def getIds(model: FinancialsTransaction): List[String] = {
    val ids: List[String]  = model.lines.map(line => PeriodicAccountBalance.createId(model.getPeriod, line.account))
    val oids: List[String] = model.lines.map(line => PeriodicAccountBalance.createId(model.getPeriod, line.oaccount))
    (ids ++ oids).distinct
  }

  private def updatePac(model: FinancialsTransaction, pacList: List[DPAC]) = {
    val dummyPac = PeriodicAccountBalance.dummy
    model.lines.flatMap { line =>
      val pacId  = PeriodicAccountBalance.createId(model.getPeriod, line.account)
      val poacId = PeriodicAccountBalance.createId(model.getPeriod, line.oaccount)
      val pac    = List(pacList.find(pac_ => pac_.id == pacId)).flatten.map(_.debiting(line.amount))
      val poac   = List(pacList.find(poac_ => poac_.id == poacId)).flatten.map(_.crediting(line.amount))
      (pac ++ poac)
    }.groupBy(_.id)
      .map { case (_, v) => reduce(v, dummyPac) }
      .filterNot(_.id == dummyPac.id)
      .toList
      .distinct

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
    model: FinancialsTransaction,
    pacList: List[DPAC]
  ): List[Journal] = model.lines.flatMap { line =>
    val pacId      = PeriodicAccountBalance.createId(model.getPeriod, line.account)
    val poacId     = PeriodicAccountBalance.createId(model.getPeriod, line.oaccount)
    val dummyPac   = PeriodicAccountBalance.dummy
    val pac: DPAC  = pacList.find(pac_ => pac_.id == pacId).getOrElse(dummyPac)
    val poac: DPAC = pacList.find(poac_ => poac_.id == poacId).getOrElse(dummyPac)
    val jou1       = makeJournal(line, model, pac, line.account, line.oaccount)
    val jou2       = makeJournal(line, model, poac, line.oaccount, line.account)
    List(jou1, jou2)
  }

}

object FinancialsServiceImpl {
  val live: ZLayer[PacRepository with TransactionRepository with JournalRepository, RepositoryError, FinancialsService] =
    ZLayer.fromFunction(new FinancialsServiceImpl(_, _, _))
}
