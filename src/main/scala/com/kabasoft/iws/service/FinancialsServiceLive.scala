package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.common.given
import com.kabasoft.iws.domain.{Account, FinancialsTransaction, FinancialsTransactionDetails, Journal, PeriodicAccountBalance, TPeriodicAccountBalance, common}
import com.kabasoft.iws.repository.{AccountRepository, FinancialsTransactionRepository, JournalRepository, PacRepository, PostFinancialsTransactionRepository}
import com.kabasoft.iws.service.FinancialsService.buildPacIds
import zio._
import scala.collection.immutable.List
import zio.prelude.FlipOps


final class FinancialsServiceLive( accRepo: AccountRepository
                                  , pacRepo: PacRepository
                                  , ftrRepo: FinancialsTransactionRepository
                                  , journalRepo: JournalRepository
                                  , repository4PostingTransaction:PostFinancialsTransactionRepository)
             extends FinancialsService:

  override def journal(accountId: String, fromPeriod: Int, toPeriod: Int, company: String): ZIO[Any, RepositoryError, List[Journal]] =
     journalRepo.find4Period(accountId, fromPeriod, toPeriod, company).map(_.toList)
  
  def getBy(id: String, company: String): ZIO[Any, RepositoryError, PeriodicAccountBalance] =
    pacRepo.getById(id, PeriodicAccountBalance.MODELID, company)

  def getByIds(ids: List[String], company: String): ZIO[Any, RepositoryError, List[PeriodicAccountBalance]] =
    pacRepo.getBy(ids, PeriodicAccountBalance.MODELID, company)

  override def postTransaction4Period(fromPeriod: Int, toPeriod: Int, modelid: Int, company: String): ZIO[Any, RepositoryError, Int] =
    for {
      models <- ftrRepo.find4Period(fromPeriod, toPeriod, modelid, company, false)
      nr     <- ZIO.foreach(models)(postTransaction).map(_.sum)
    } yield nr

  override def postAll(ids: List[Long], modelid:Int, company: String): ZIO[Any, RepositoryError, Int] =
    for {
      queries <- ftrRepo.getBy(ids, modelid, company)
      _ <- ZIO.logDebug(s"Posting transaction  ${queries}")
      models = queries.filter(_.posted == false)
      _ <- ZIO.logDebug(s"Posting models  ${models}")
      _ <-ZIO.foreachDiscard(models.map(_.id)) (id =>ZIO.logDebug(s"Posting transaction with id ${id} of company ${company}"))
      nr <- ZIO.foreach(models)(postTransaction).map(_.sum)
    } yield nr

  override def post(id: Long, company: String): ZIO[Any, RepositoryError, Int] =
    ZIO.logDebug(s" Posting transaction with id ${id} of company ${company}") *>
      ftrRepo
        .getByTransId((id, company))
        .flatMap(postTransaction)

  override def postNewFinancialsTransaction(transaction: FinancialsTransaction): ZIO[Any, RepositoryError,
    (FinancialsTransaction, List[PeriodicAccountBalance], UIO[List[PeriodicAccountBalance]], List[Journal])] = {
    val model = transaction.copy(period = common.getPeriod(transaction.transdate))
    val pacids = buildPacIds(model)
    val company = transaction.company
    for {
      pacs <- pacRepo.getBy(pacids, PeriodicAccountBalance.MODELID, company).map(_.filterNot(_.id.equals(PeriodicAccountBalance.dummy.id)))
      newPacs = PeriodicAccountBalance.create(model).filterNot(pac => pacs.map(_.id).contains(pac.id))
        .groupBy(_.id) map { case (_, v) => common.reduce(v, PeriodicAccountBalance.dummy) }
      tpacs <- pacs.map(TPeriodicAccountBalance.apply).flip
      oldPacs <- updatePac(model, tpacs).map(e => e.map(PeriodicAccountBalance.applyT))
      accounts <- accRepo.getBy(model.lines.flatMap(line => List(line.account, line.oaccount)), Account.MODELID, model.company)
      journalEntries <- makeJournal(model, newPacs.toList, oldPacs.flip, accounts)
    } yield (model, newPacs.toList, oldPacs.flip, journalEntries)
  }
  
  private def postTransaction(transaction: FinancialsTransaction): ZIO[Any, RepositoryError, Int] = {
    val model = transaction.copy(period = common.getPeriod(transaction.transdate))
    val pacids = buildPacIds(model)
    val company = transaction.company
    for {
      pacs <- pacRepo.getBy(pacids, PeriodicAccountBalance.MODELID, company).map(_.filterNot(_.id.equals(PeriodicAccountBalance.dummy.id)))
      newRecords = PeriodicAccountBalance.create(model).filterNot(pac => pacs.map(_.id).contains(pac.id))
        .groupBy(_.id) map { case (_, v) => common.reduce(v, PeriodicAccountBalance.dummy)}
      tpacs <- pacs.map(TPeriodicAccountBalance.apply).flip
      oldPacs <- updatePac(model, tpacs).map(e=>e.map(PeriodicAccountBalance.applyT))
      accounts <- accRepo.getBy(model.lines.flatMap(line=>List(line.account, line.oaccount)), Account.MODELID, model.company)
      journalEntries <- makeJournal(model, newRecords.toList, oldPacs.flip, accounts)
      post <- repository4PostingTransaction.post(List.empty[FinancialsTransaction], List(model), newRecords.toList, oldPacs.flip, journalEntries)
    } yield post
  }
  
  private def updatePac(model: FinancialsTransaction, tpacs: List[TPeriodicAccountBalance]): ZIO[Any, Nothing, List[TPeriodicAccountBalance]] = {
    val result = for {
      newRecords <- groupById(PeriodicAccountBalance.create(model)).map(TPeriodicAccountBalance.apply).flip
      _ <- newRecords.map(pac => transfer(pac, tpacs)).flip
    } yield (if (tpacs.nonEmpty) tpacs else newRecords)
    result
  }

  private def transfer( pac:TPeriodicAccountBalance,  tpacs:List[TPeriodicAccountBalance]): ZIO[Any, Nothing, Option[Unit]] =
    tpacs.find(_.id ==  pac.id).map(pac_ => pac_.transfer(pac, pac_)).flip

  private def groupById(r: List[PeriodicAccountBalance]) = 
    r.groupBy(_.id).map { case (_, v) => common.reduce(v, PeriodicAccountBalance.dummy)}.toList
  
  def findOrBuildPac(pacId: String, period_ : Int, pacList: List[PeriodicAccountBalance]): PeriodicAccountBalance =
    pacList.find(pac_ => pac_.id == pacId).getOrElse(PeriodicAccountBalance.dummy.copy(id = pacId, period = period_))
    
  private def makeJournal(model: FinancialsTransaction,  pacListx: List[PeriodicAccountBalance]
                          , tpacList: UIO[List[PeriodicAccountBalance]]
                         , accounts:List[Account]): ZIO[Any, Nothing, List[Journal]] = for {
    pacList<-tpacList
    journal = model.lines.flatMap { line =>
      val pacId = PeriodicAccountBalance.createId(model.getPeriod, line.account)
      val poacId = PeriodicAccountBalance.createId(model.getPeriod, line.oaccount)
      val pac = findOrBuildPac(pacId, model.getPeriod, pacList++pacListx)
      val poac = findOrBuildPac(poacId, model.getPeriod, pacList++pacListx)
      val jou1 = buildJournalEntries(model, line, pac, accounts, line.account, line.oaccount, line.side)
      val jou2 = buildJournalEntries(model, line, poac, accounts, line.oaccount, line.account, !line.side)
      List(jou1, jou2)
    }
  } yield journal
  private def buildJournalEntries(model: FinancialsTransaction, line: FinancialsTransactionDetails,
                                  pac: PeriodicAccountBalance, accounts:List[Account], account: String, oaccount: String, side:Boolean) = {
    val parentAccountId = accounts.find(_.account == account).fold("")(_.id)
    val parentOAccountId = accounts.find(_.account == oaccount).fold("")(_.id)
    Journal(
      -1,
      model.id,
      model.oid,
      account,
      oaccount,
      parentAccountId,
      parentOAccountId,
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


object FinancialsServiceLive:
  val live: ZLayer[AccountRepository&PacRepository& FinancialsTransactionRepository& JournalRepository& PostFinancialsTransactionRepository
    , RepositoryError, FinancialsService] =
    ZLayer.fromFunction(new FinancialsServiceLive(_, _, _, _,_))
