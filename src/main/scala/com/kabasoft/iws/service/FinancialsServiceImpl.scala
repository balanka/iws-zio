package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.common._
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
  type PType     = (List[DPAC], FinancialsTransaction) => List[DPAC]

  override def postAll(ids: List[Long], company: String): ZIO[Any, RepositoryError, List[Int]] =
    for {
      queries <- ZIO.foreach(ids)(id => ftrRepo.getBy(id.toString, company))
      models   = queries.map(FinancialsTransaction.apply1).filter(_.posted == false)
      all     <- ZIO.foreach(models)(debitOrCreditPACAll(_, company))
    } yield all

  override def postTransaction4Period(
    fromPeriod: Int,
    toPeriod: Int,
    company: String
  ): ZIO[Any, RepositoryError, List[Int]] =
    for {
      queries                            <- ftrRepo.find4Period(fromPeriod, toPeriod, company).runCollect
      models: List[FinancialsTransaction] = queries.toList.map(FinancialsTransaction.apply1)
      all                                <- ZIO.foreach(models)(trans => debitOrCreditPACAll(trans, company))
    } yield all

  override def post(model: DerivedTransaction, company: String): ZIO[Any, RepositoryError, List[Int]] =
    postAll(List(model.lid), company)

  private[this] def debitOrCreditPACAll(model: FinancialsTransaction, company: String): ZIO[Any, RepositoryError, Int] =
    for {
      pac           <- ZIO.foreach(getIds(model))(pacRepo.getBy(_, company))
      oldRecords     = getPeriodicAccount(pac, model, getAndDebitCreditOldPacs)
      newRecords     = getPeriodicAccount(pac, model, getAndDebitCreditNewPacs)
      journalEntries = newJournalEntries(model, oldRecords ::: newRecords)
      pac_created   <- pacRepo.create(newRecords)
      pac_updated   <- pacRepo.modify(oldRecords)
      model_         = model.copy(posted = true).copy(postingdate = Instant.now())
      trans_posted  <- ftrRepo.modify(model_)
      journal       <- journalRepo.create(journalEntries)
    } yield pac_created + pac_updated + journal + trans_posted

  private[this] def getIds(model: FinancialsTransaction): List[String] = {
    val ids: List[String]  = model.lines.map(line => PeriodicAccountBalance.createId(model.getPeriod, line.account))
    val oids: List[String] = model.lines.map(line => PeriodicAccountBalance.createId(model.getPeriod, line.oaccount))
    println("newRecords: ids++oids" + ids ++ oids)
    (ids ++ oids).distinct
  }

  private[this] def getPeriodicAccount(pacList: List[DPAC], model: FinancialsTransaction, getPac: PType): List[DPAC] = {

    val pacs: List[DPAC]   = getPac(pacList, model)
    val result: List[DPAC] = pacs
      .groupBy(_.id)
      .map { case (_, v) => reduce(v, PeriodicAccountBalance.dummy) }
      .toSet
      .toList
      .filterNot(x => x.id == PeriodicAccountBalance.dummy.id)
    println("result: " + result)
    result
  }

  def getOldPacs(pacList: List[DPAC], period: Int, acc: String): Option[DPAC]    = {
    val pacId = PeriodicAccountBalance.createId(period, acc)
    pacList.toSet.find(pac_ => pac_.id == pacId)
  }
  def debitIt(packList: List[DPAC], period: Int, line: FTDetails): Option[DPAC]  =
    packList.find(pac_ => pac_.id == PeriodicAccountBalance.createId(period, line.account)).map(_.debiting(line.amount))
  def creditIt(packList: List[DPAC], period: Int, line: FTDetails): Option[DPAC] =
    packList
      .find(pac_ => pac_.id == PeriodicAccountBalance.createId(period, line.oaccount))
      .map(_.crediting(line.amount))

  val getAndDebitCreditOldPacs: PType = (pacList: List[DPAC], model: FinancialsTransaction) => {
    val pacx1: List[DPAC]  = model.lines.flatMap(line => getOldPacs(pacList, model.period, line.account)).distinct
    val poacx1: List[DPAC] = model.lines.flatMap(line => getOldPacs(pacList, model.period, line.oaccount)).distinct
    buildPacList(model, pacx1, poacx1)
  }

  private def buildPacList(model: FinancialsTransaction, pacx1: List[DPAC], poacx1: List[DPAC]) = {
    val groupedLines: List[FTDetails]  =
      model.lines.groupBy(_.account).map { case (_, v) => reduce(v, FinancialsTransactionDetails.dummy) }.toList
    val groupedOLines: List[FTDetails] =
      model.lines.groupBy(_.oaccount).map { case (_, v) => reduce(v, FinancialsTransactionDetails.dummy) }.toList
    val pacx: List[DPAC]               = groupedLines.flatMap(line => debitIt(pacx1, model.period, line))
    val poacx: List[DPAC]              = groupedOLines.flatMap(line => creditIt(poacx1, model.period, line))

    Set(pacx, poacx).flatten.toList
  }

  val getAndDebitCreditNewPacs: PType                                                                                  = (pacList: List[DPAC], model: FinancialsTransaction) => {
    val pacx1                                 = model.lines.map(line => createIfNone(pacList, model.period, line, line.account, model.company))
    val pacx1x: List[DPAC]                    = pacx1.filter(_._2 == true).flatMap(m => m._1).distinct
    val poacx1: List[(Option[DPAC], Boolean)] = model.lines
      .map(line => createIfNone(pacList, model.period, line, line.oaccount, model.company))
    val poacx1x: List[DPAC]                   = poacx1.filter(_._2 == true).flatMap(m => m._1).distinct
    buildPacList(model, pacx1x, poacx1x)

  }

  def createIfNone(
    pacList: List[DPAC],
    period: Int,
    line: FTDetails,
    accountId: String,
    company: String
  ): (Option[DPAC], Boolean) = {
    val pacId                                                                              = PeriodicAccountBalance.createId(period, accountId)
    val foundPac: Option[DPAC]                                                             = pacList.toSet.find(pac_ => pac_.id == pacId)
    def f(line: FTDetails, pac: DPAC): Option[DPAC]                                        =
      if (
        (line.account == pac.account && line.side)
        || (line.oaccount == pac.account && !line.side)
      )
        Some(pac)
      else None
    def fx(line: FTDetails, period: Int, accountId: String, company: String): Option[DPAC] =
      if (line.account == accountId) Some(PeriodicAccountBalance.create(line.account, period, line.currency, company))
      else if (line.oaccount == accountId)
        Some(PeriodicAccountBalance.create(line.oaccount, period, line.currency, company))
      else None

    foundPac match {
      case Some(pac) => (f(line, pac), false)
      case None      => (fx(line, period, accountId, company), true)
    }
  }
  private def makeJournal(line: FTDetails, model: FinancialsTransaction, pac: DPAC, account: String, oaccount: String) =
    Journal(
      -1,
      model.tid,
      // model.oid,
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
      // line.side,
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
  private[this] def newJournalEntries(model: FinancialsTransaction, pacList: List[DPAC]): List[Journal]                =
    model.lines.flatMap(line => createJournalEntries(line, model, pacList))

}

object FinancialsServiceImpl {
  val live
    : ZLayer[PacRepository with TransactionRepository with JournalRepository, RepositoryError, FinancialsService] =
    ZLayer.fromFunction(new FinancialsServiceImpl(_, _, _))
}
