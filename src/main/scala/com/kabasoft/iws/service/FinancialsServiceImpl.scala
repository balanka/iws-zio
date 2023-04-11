package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.common._
import com.kabasoft.iws.domain.{
  common,
  FinancialsTransaction,
  FinancialsTransactionDetails,
  Journal,
  PeriodicAccountBalance,
  TPeriodicAccountBalance
}
import com.kabasoft.iws.repository.{ JournalRepository, PacRepository, TransactionRepository }
import zio._
import zio.prelude.FlipOps

import java.time.Instant

final class FinancialsServiceImpl(pacRepo: PacRepository, ftrRepo: TransactionRepository, journalRepo: JournalRepository) extends FinancialsService {
  type DPAC      = TPeriodicAccountBalance
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
      nr     <- postAllTransactions(models.toList, company).flatten
     // nr     <- ZIO.foreach(models)(trans => postTransaction(trans, company)).map(_.toList)
    } yield nr

  override def postAll(ids: List[Long], company: String): ZIO[Any, RepositoryError, Int] =
    for {
      queries <- ZIO.foreach(ids)(id => ftrRepo.getByTransId((id, company)))
      models = queries.filter(_.posted == false)
      nr <- postAllTransactions(models, company).flatten
    } yield nr

  override def post(id: Long, company: String): ZIO[Any, RepositoryError, Int] =
    ftrRepo
      .getByTransId((id, company))
      .flatMap(trans => postAllTransactions(List(trans), company).flatten)

  private[this] def prepare(transactions: List[FinancialsTransaction], company: String): ZIO[Any, RepositoryError, (ZIO[Any, Nothing, List[PeriodicAccountBalance]], ZIO[Any, Nothing, List[PeriodicAccountBalance]])] = {
    val models = transactions.map(trans =>trans.copy(period = common.getPeriod(trans.transdate)))
    for {
      pacs <- pacRepo.getByIds(buildPacIds(models), company)
      tpacs <- ZIO.foreach(pacs)(TPeriodicAccountBalance.apply)
      xx = models.flatMap(PeriodicAccountBalance.create).filterNot(pacs.contains).groupBy(_.id) map { case (_, v) =>
        common.reduce(v, PeriodicAccountBalance.dummy) // .copy(id = k)
      }
      newRecords <- ZIO.foreach(xx.toList)(TPeriodicAccountBalance.apply)
      newPacs = updatePac(models, newRecords)
      oldPacs = updatePac(models, tpacs)
      newTPacs = newPacs.map(PeriodicAccountBalance.applyT).flip
      oldTPacs = oldPacs.map(PeriodicAccountBalance.applyT).flip

    } yield (newTPacs, oldTPacs)
  }

  private[this] def postTransaction(transaction: FinancialsTransaction,  allPacs:(UIO[List[PeriodicAccountBalance]], UIO[List[PeriodicAccountBalance]]) )= {
    val model = transaction.copy(period = common.getPeriod(transaction.transdate))
    for {
       newPacs <-  allPacs._1
       oldPacs <-  allPacs._2
      journalEntries = makeJournal(model, newPacs ++ oldPacs)
      transactions = model.copy(posted = true, postingdate = Instant.now())
    } yield (journalEntries, List(transactions), newPacs, oldPacs)
  }

  private[this] def postAllTransactions(models: List[FinancialsTransaction], company: String)=for {
   pacs<- prepare(models, company)
   //pacs = pacsx.flatten.distinct
   entries <- ZIO.foreach(models)(postTransaction(_,  pacs))
  }yield {
  val (journals,transactions, newPacs, oldPacs) = entries.reduce((x,y) => (x._1++y._1,x._2++y._2, x._3++y._3,x._4++y._4))
    persist(journals, transactions, newPacs, oldPacs)
  }

  private[this] def buildPacId(period: Int, accountId: String): String =
    PeriodicAccountBalance.createId(period, accountId)

  private[this] def buildPacIds(models: List[FinancialsTransaction]): List[String] = models.flatMap(model => {
    model.lines.map(line => buildPacId(model.getPeriod, line.account)) ++
      model.lines.map(line => buildPacId(model.getPeriod, line.oaccount))
  }).distinct

  private def persist( journalEntries:List[Journal], transactions:List[FinancialsTransaction],
                                  newPacs: List[PeriodicAccountBalance], oldPacs:List[PeriodicAccountBalance]) = for{

    pac_created <-  pacRepo.create(newPacs)
    _           <- ZIO.logDebug(s" number of  pacs created  posted ${pac_created}")
    pac_updated <- pacRepo.modify(oldPacs)
    _           <- ZIO.logDebug(s" number of  pacs updated  posted ${pac_updated}")
    journals_ <- journalRepo.create(journalEntries)
    _          <- ZIO.logDebug(s"Created  ${journals_} journal entries")
    trans = transactions.map(_.copy(posted = true, postingdate = Instant.now()))
    trans_posted <- ftrRepo.modify(trans)
    _            <- ZIO.logDebug(s" number of  pacs updated  posted ${pac_updated}")

  }yield pac_created+ pac_updated+trans_posted+journals_

  private def updatePac(models: List[FinancialsTransaction], tpacs: List[DPAC]) = models.flatMap(model =>
      model.lines.flatMap( line => {
        val pacs = tpacs.filter(pac_ => pac_.id == buildPacId(model.getPeriod, line.account))
        val poacs = tpacs.filter(poac_ => poac_.id == buildPacId(model.getPeriod, line.oaccount))
        TPeriodicAccountBalance.debitAndCreditAll(pacs, poacs, line.amount)
        List(pacs, poacs)
      })
  ).flatten

  private def makeJournal(model: FinancialsTransaction, pacList: List[PeriodicAccountBalance]): List[Journal] = for {
    journal <- model.lines.flatMap { line =>
                 val pac: Option[PeriodicAccountBalance]  = pacList.find(pac_ => pac_.id == buildPacId(model.getPeriod, line.account))
                 val poac: Option[PeriodicAccountBalance] = pacList.find(poac_ => poac_.id == buildPacId(model.getPeriod, line.oaccount))
                 val jou1                                 = pac.map(buildJournalEntries(model, line, _, line.account, line.oaccount))
                 val jou2                                 = poac.map(buildJournalEntries(model, line, _, line.oaccount, line.account))
                 List(jou1, jou2).flatten
               }
  } yield journal

  private def buildJournalEntries(model: FinancialsTransaction, line: FTDetails, pac: PeriodicAccountBalance, account: String, oaccount: String) =
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
      line.side,
      line.text,
      model.month.toInt,
      model.year,
      model.company,
      // model.file_content,
      model.modelid
    )
}

object FinancialsServiceImpl {
  val live: ZLayer[PacRepository with TransactionRepository with JournalRepository, RepositoryError, FinancialsService] =
    ZLayer.fromFunction(new FinancialsServiceImpl(_, _, _))
}
