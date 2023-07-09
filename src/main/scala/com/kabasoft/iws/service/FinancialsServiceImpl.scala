package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.common._
import com.kabasoft.iws.domain.{FinancialsTransaction, FinancialsTransactionDetails, Journal, PeriodicAccountBalance, TPeriodicAccountBalance, common}
import com.kabasoft.iws.repository.{JournalRepository, PacRepository, TransactionRepository}
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
    ZIO.logInfo(s" Posting transaction with id ${id} of company ${company}")*>
    ftrRepo
      .getByTransId((id, company))
      .flatMap(trans => postAllTransactions(List(trans), company).flatten)

  private[this] def prepare(transactions: List[FinancialsTransaction], company: String): ZIO[Any, RepositoryError, (ZIO[Any, Nothing, List[PeriodicAccountBalance]], ZIO[Any, Nothing, List[PeriodicAccountBalance]])] = {
    val models = transactions.map(trans =>trans.copy(period = common.getPeriod(trans.transdate)))
    for {
      pacs <- pacRepo.getByIds(buildPacIds(models), company)
      tpacs <- ZIO.foreach(pacs)(TPeriodicAccountBalance.apply)
      _<- ZIO.foreachDiscard(pacs)(pac =>ZIO.logInfo(s"Id of pac FOUND ${pac.id}  ${company}"))
      //newRecords     = PeriodicAccountBalance.create(model).filterNot(pacs.contains).distinct
      newPacs_ = models.flatMap(PeriodicAccountBalance.create).filterNot(pacs.contains).distinct.groupBy(_.id) map { case (_, v) =>
        common.reduce(v, PeriodicAccountBalance.dummy)
      }
      _<- ZIO.foreachDiscard(newPacs_)(pac =>ZIO.logInfo(s"Ids of new pac CREATED ${pac.id}  ${company}"))
      newRecords <- ZIO.foreach(newPacs_.toList)(TPeriodicAccountBalance.apply)
      newTPacs = updatePac(models, newRecords.distinct)
      oldTPacs = updatePac(models, tpacs)
      newPacs = newTPacs.map(PeriodicAccountBalance.applyT).flip
      oldPacs = oldTPacs.map(PeriodicAccountBalance.applyT).flip

    } yield (newPacs, oldPacs)
  }

  private[this] def postTransaction(transaction: FinancialsTransaction,  allPacs:(UIO[List[PeriodicAccountBalance]], UIO[List[PeriodicAccountBalance]]) ): ZIO[Any, Nothing, (List[Journal],  List[PeriodicAccountBalance], List[PeriodicAccountBalance])] = {
    val model = transaction.copy(period = common.getPeriod(transaction.transdate))
    for {
       newPacs <-  allPacs._1
       oldPacs <-  allPacs._2
      journalEntries = makeJournal(model, newPacs ++ oldPacs)
    } yield (journalEntries,  newPacs, oldPacs)
  }

  private[this] def postAllTransactions(models: List[FinancialsTransaction], company: String)=for {
   pacs<- prepare(models, company)
   //pacs = pacsx.flatten.distinct
   entries <- ZIO.foreach(models)(postTransaction(_,  pacs))
  }yield {
  val (journals, newPacs, oldPacs) = entries.reduce((x,y) => (x._1++y._1, x._2++y._2, x._3++y._3))
    val transactions = models.map(ftr=>ftr.copy(posted=true, postingdate = Instant.now(), period=common.getPeriod(ftr.transdate)))
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
    _           <- ZIO.logInfo(s" number of  pacs created  posted ${pac_created}")
    pac_updated <- pacRepo.modify(oldPacs)
    _           <- ZIO.logInfo(s" number of  pacs updated  posted ${pac_updated}")
    journals_ <- journalRepo.create(journalEntries)
    _          <- ZIO.logInfo(s"Created  ${journals_} journal entries")
    trans = transactions.map(_.copy(posted = true, postingdate = Instant.now()))
    _          <- ZIO.foreachDiscard(transactions)(trans =>ZIO.logInfo(s"Transaction posted  ${trans}"))

    trans_posted <- ftrRepo.updatePostedField(trans)
    _            <- ZIO.logInfo(s" number of  pacs updated  posted ${pac_updated}")

  }yield pac_created+ pac_updated+trans_posted+journals_

  private def updatePac(models: List[FinancialsTransaction], tpacs: List[DPAC]) = models.flatMap(model =>
      model.lines.flatMap( line => {
        val pacs = tpacs.filter(pac_ => pac_.id == buildPacId(model.getPeriod, line.account))
        val poacs = tpacs.filter(poac_ => poac_.id == buildPacId(model.getPeriod, line.oaccount))
        pacs.zip(poacs).map { case (pac, poac) => pac.transfer(poac, line.amount)}
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
