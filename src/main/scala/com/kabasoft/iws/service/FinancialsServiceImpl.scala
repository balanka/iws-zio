package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.common._
import com.kabasoft.iws.domain.{FinancialsTransaction, FinancialsTransactionDetails, Journal, PeriodicAccountBalance, TPeriodicAccountBalance, common}
import com.kabasoft.iws.repository.{JournalRepository, PacRepository, PostTransactionRepository, TransactionRepository}
import zio._



final class FinancialsServiceImpl(pacRepo: PacRepository, ftrRepo: TransactionRepository, journalRepo: JournalRepository, repository4PostingTransaction:PostTransactionRepository) extends FinancialsService {
  type DPAC = TPeriodicAccountBalance
  type FTDetails = FinancialsTransactionDetails

  override def create(model: FinancialsTransaction): ZIO[Any, RepositoryError, Int] =
    ftrRepo.create(model)


  override def create(items: List[FinancialsTransaction]): ZIO[Any, RepositoryError, Int] =
    ftrRepo.create(items)

  override def journal(accountId: String, fromPeriod: Int, toPeriod: Int, company: String): ZIO[Any, RepositoryError, List[Journal]] =
    for {
      queries <- journalRepo.find4Period(accountId, fromPeriod, toPeriod, company).runCollect.map(_.toList)
    } yield queries

  def getBy(id: String, company: String): ZIO[Any, RepositoryError, PeriodicAccountBalance] =
    pacRepo.getBy(id, company)

  def getByIds(ids: List[String], company: String): ZIO[Any, RepositoryError, List[PeriodicAccountBalance]] =
    pacRepo.getByIds(ids, company)

  override def postTransaction4Period(fromPeriod: Int, toPeriod: Int, company: String): ZIO[Any, RepositoryError, Int] =
    for {
      models <- ftrRepo
        .find4Period(fromPeriod, toPeriod, company)
        .filter(_.posted == false)
        .runCollect
      nr <- ZIO.foreach(models)(trans => postTransaction(trans, company)).map(_.toList).map(_.sum)
    } yield nr

  override def postAll(ids: List[Long], company: String): ZIO[Any, RepositoryError, Int] =
    for {
      queries <- ZIO.foreach(ids)(id => ftrRepo.getByTransId((id, company)))
      models = queries.filter(_.posted == false)
      nr <- ZIO.foreach(models)(model => postTransaction(model, company)).map(_.sum)
    } yield nr

  override def post(id: Long, company: String): ZIO[Any, RepositoryError, Int] =
    ZIO.logInfo(s" Posting transaction with id ${id} of company ${company}") *>
      ftrRepo
        .getByTransId((id, company))
        .flatMap(trans => postTransaction(trans, company))

  private[this] def postTransaction(transaction: FinancialsTransaction, company: String): ZIO[Any, RepositoryError, Int] = {
    val model = transaction.copy(period = common.getPeriod(transaction.transdate))
    for {
      pacs <- pacRepo.getByIds(buildPacIds(model), company)
      _ <- ZIO.logInfo(s" pacs ${pacs}")
      newRecords = PeriodicAccountBalance.create(model).filterNot(pac => pacs.map(_.id).contains(pac.id))
        .groupBy(_.id) map { case (_, v) =>
        common.reduce(v, PeriodicAccountBalance.dummy) // .copy(id = k)
      }
      _ <- ZIO.logInfo(s" newRecords ${newRecords}")
      oldPacs = updatePac(model, pacs)
      _ <- ZIO.logInfo(s" oldPacs ${oldPacs}")
      journalEntries = makeJournal(model, newRecords.toList ++ pacs)
      _ <- ZIO.logInfo(s" journalEntries ${journalEntries}")
      post <- repository4PostingTransaction.post(List(model), newRecords.toList, oldPacs, journalEntries)

    } yield post

  }

  private[this] def buildPacId(period: Int, accountId: String): String =
    PeriodicAccountBalance.createId(period, accountId)

  private[this] def buildPacIds(model: FinancialsTransaction): List[String] = {
    val pacIds: List[String] = model.lines.map(line => buildPacId(model.getPeriod, line.account))
    val pacOids: List[String] = model.lines.map(line => buildPacId(model.getPeriod, line.oaccount))
    (pacIds ++ pacOids).distinct
  }

  private def updatePac(model: FinancialsTransaction, pacs: List[PeriodicAccountBalance]): List[PeriodicAccountBalance] = {
    val r0:List[PeriodicAccountBalance] = groupById(PeriodicAccountBalance.create(model))
    val r:List[PeriodicAccountBalance] = r0.map { pac =>
      pacs.find(_.id ==  pac.id).map(pac_ => pac_.copy(idebit = pac_.idebit.add(pac.idebit), debit = pac_.debit.add(pac.debit)
        , icredit = pac_.icredit.add(pac.icredit), credit = pac_.credit.add(pac.credit)))
      .getOrElse(PeriodicAccountBalance.dummy)
    }
      //TPeriodicAccountBalance.debitAndCreditAllX(pacs, poacs, line.amount)
      val result =  groupById(r)
    result
  }


  private def groupById(r: List[PeriodicAccountBalance]) = {
   val r0 = (r.groupBy(_.id) map { case (_, v) =>
      common.reduce(v, PeriodicAccountBalance.dummy)
    }).toList//.filterNot(_.id == PeriodicAccountBalance.dummy.id)
    r0
  }

  private def makeJournal(model: FinancialsTransaction, pacList: List[PeriodicAccountBalance]): List[Journal] = for {
    journal <- model.lines.flatMap { line =>
                 val pac: Option[PeriodicAccountBalance]  = pacList.find(pac_ => pac_.id == buildPacId(model.getPeriod, line.account))
                 val poac: Option[PeriodicAccountBalance] = pacList.find(poac_ => poac_.id == buildPacId(model.getPeriod, line.oaccount))
                 val jou1                                 = pac.map(buildJournalEntries(model, line, _, line.account, line.oaccount, line.side))
                 val jou2                                 = poac.map(buildJournalEntries(model, line, _, line.oaccount, line.account, !line.side))
                 List(jou1, jou2).flatten
               }
  } yield journal

  private def buildJournalEntries(model: FinancialsTransaction, line: FTDetails, pac: PeriodicAccountBalance, account: String, oaccount: String,side:Boolean) =
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
}

object FinancialsServiceImpl {
  val live: ZLayer[PacRepository with TransactionRepository with JournalRepository with PostTransactionRepository, RepositoryError, FinancialsService] =
    ZLayer.fromFunction(new FinancialsServiceImpl(_, _, _, _))
}
