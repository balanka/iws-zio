package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.common._
import com.kabasoft.iws.domain.{FinancialsTransaction, FinancialsTransactionDetails, Journal, PeriodicAccountBalance, TPeriodicAccountBalance, common}
import com.kabasoft.iws.repository.{JournalRepository, PacRepository, TransactionRepository}
import zio._
//import zio.prelude.FlipOps


import java.time.Instant

final class FinancialsServiceImpl(pacRepo: PacRepository, ftrRepo: TransactionRepository, journalRepo: JournalRepository) extends FinancialsService {
  type DPAC      = TPeriodicAccountBalance
  type FTDetails = FinancialsTransactionDetails
  type A =(List[Journal],List[PeriodicAccountBalance], List[PeriodicAccountBalance])

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
      nr     <- postAllTransactions(models.toList, company)
     // nr     <- nr_
    } yield nr

  override def postAll(ids: List[Long], company: String): ZIO[Any, RepositoryError, Int] =
    for {
      queries <- ZIO.foreach(ids)(id => ftrRepo.getByTransId((id, company)))
      models = queries.filter(_.posted == false)
      nr <- postAllTransactions(models, company)
    } yield nr

  override def post(id: Long, company: String): ZIO[Any, RepositoryError, Int] =
    ZIO.logInfo(s" Posting transaction with id ${id} of company ${company}")*>
    ftrRepo
      .getByTransId((id, company))
      .flatMap(trans => postAllTransactions(List(trans), company))

  private[this] def prepare(transactions: List[FinancialsTransaction], company: String) = {
    val models = transactions.map(trans =>trans.copy(period = common.getPeriod(trans.transdate)))
    val pacIds = buildPacIds(models)
    for {

      pacs <- pacRepo.getByIds(pacIds, company)
      //tpacs <- ZIO.foreach(pacs)(TPeriodicAccountBalance.apply)
      _<- ZIO.foreachDiscard(pacs)(pac =>ZIO.logInfo(s"Id of pac FOUND ${pac.id}  ${company}"))
      //newRecords     = PeriodicAccountBalance.create(model).filterNot(pacs.contains).distinct
     // newPacs_ = models.flatMap(PeriodicAccountBalance.create).filterNot(pacs.contains).distinct.groupBy(_.id) map { case (_, v) =>
      allPacs =  models.flatMap(TPeriodicAccountBalance.create)//.filterNot(pac=>pacs.map(_.id).contains(pac.id)).distinct//.groupBy(_.id) map { case (_, v) =>
       // common.reduce(v, PeriodicAccountBalance.dummy)
      //}
      _<- ZIO.foreachDiscard(allPacs)(pac =>ZIO.logInfo(s"Ids of new pac CREATED ${pac}"))
      oldPacs1 = (allPacs++pacs).groupBy(_.id) map { case (_, v) => common.reduce(v, PeriodicAccountBalance.dummy)}
      oldPacs = allPacs.filter(pac=>pacs.map(_.id).contains(pac.id))
      oldPacs_ = allPacs.intersect(pacs)
      intersect = oldPacs.toSet.intersect(pacs.toSet)
      oldPacsX = oldPacs.groupBy (_.id) map { case (_, v) =>
       common.reduce(v, PeriodicAccountBalance.dummy)
      }
      newPacs_ = allPacs.diff(oldPacs)
      _<- ZIO.logInfo(s" Pacs ${pacs}")
      _<- ZIO.logInfo(s" allPacs ${allPacs}")
      _<- ZIO.logInfo(s" oldPacs1 ${oldPacs1}")
      _<- ZIO.logInfo(s" oldPacsX ${oldPacsX}")
      _<- ZIO.logInfo(s"intersect oldPacs_ ${oldPacs_}")
      _<- ZIO.logInfo(s"intersect oldpacs ${intersect}")
      _<- ZIO.logInfo(s"Diff newPacs_ ${newPacs_}")
      newPacs = allPacs.filterNot(pac=>oldPacs.map(_.id).contains(pac.id))


      //newTPacs = updatePac(models, newRecords.distinct)
//      newTPacs = updatePac(models, newPacs_.++tpacs)
//      oldTPacs = updatePac(models, newPacs_.toList++tpacs)
//      newPacs = newTPacs.map(PeriodicAccountBalance.applyT).flip
//      oldPacs = oldTPacs.map(PeriodicAccountBalance.applyT).flip

    } yield (newPacs, oldPacs1.toList)
  }

  private[this] def postTransaction(transactions: List[FinancialsTransaction],  allPacs:(List[PeriodicAccountBalance], List[PeriodicAccountBalance]) ) =
    transactions.map(model=>(makeJournal(model, allPacs._1 ++ allPacs._2), allPacs._1 , allPacs._2)).fold[A]((List(), List(),List())){
      (e1:A,e2:A)=>(e1._1++e2._1, e1._2++e2._2,e1._3++e2._3)
    }

  private[this] def postAllTransactions(models: List[FinancialsTransaction], company: String): ZIO[Any, RepositoryError, Int] =for {
   pacs<- prepare(models, company)
   //pacs = pacsx.flatten.distinct
   transactions = models.map(ftr=>ftr.copy(posted=true, postingdate = Instant.now(), period=common.getPeriod(ftr.transdate)))
   entries = postTransaction(models,  pacs)
   result <-persist(entries,transactions)

  }yield result


  private[this] def buildPacId(period: Int, accountId: String): String =
    PeriodicAccountBalance.createId(period, accountId)

  private[this] def buildPacIds(models: List[FinancialsTransaction]): List[String] = models.flatMap(model => {
    model.lines.map(line => buildPacId(model.getPeriod, line.account)) ++
      model.lines.map(line => buildPacId(model.getPeriod, line.oaccount))
  }).distinct

  private def persist( entries: (List[Journal],List[PeriodicAccountBalance],List[PeriodicAccountBalance]),
                        models: List[FinancialsTransaction]): ZIO[Any, RepositoryError, Int] = {
    val trans = models.map(ftr=>ftr.copy(posted=true, postingdate = Instant.now(), period=common.getPeriod(ftr.transdate)))
    for {
     // result <- persist2(trans, entries._1, entries._2,entries._3)
      pac_created <- pacRepo.create(entries._2)
      _ <- ZIO.logInfo(s" number of  pacs created  posted ${pac_created} ${entries._2}")
      pac_updated <- pacRepo.modify(entries._3)
      _ <- ZIO.logInfo(s" number of  pacs updated  posted ${pac_updated} ${entries._3}")
      journals_ <- journalRepo.create(entries._1)
      _ <- ZIO.logInfo(s"Created  ${journals_} journal entries")
      trans_posted <- ftrRepo.modify(trans)
      _ <- ZIO.logInfo(s" number of  transaction  posted ${trans_posted}")
    }yield pac_created + pac_updated  + journals_ + trans_posted
  }


/*
  private def persist2(models: List[FinancialsTransaction], journalEntries: List[Journal],
                      newPacs: List[PeriodicAccountBalance], oldPacs: List[PeriodicAccountBalance]) = for {


    pac_created <- pacRepo.create(newPacs)
    _ <- ZIO.logInfo(s" number of  pacs created  posted ${pac_created} ${newPacs}")
    pac_updated <- pacRepo.modify(oldPacs)
    _ <- ZIO.logInfo(s" number of  pacs updated  posted ${pac_updated} ${oldPacs}")
    journals_ <- journalRepo.create(journalEntries)
    _ <- ZIO.logInfo(s"Created  ${journals_} journal entries")
    trans_posted <- ftrRepo.updatePostedField(models)
    _            <- ZIO.logInfo(s" number of  transaction  posted ${trans_posted}")
     } yield pac_created + pac_updated  + journals_ + trans_posted
*/


/*  private def updatePac(models: List[FinancialsTransaction], tpacs: List[DPAC]) = {
    val r =models.flatMap(model =>
      model.lines.flatMap( line => {
        val pacId = buildPacId(model.getPeriod, line.account)
        val poacId = buildPacId(model.getPeriod, line.oaccount)
        val pacs = tpacs.find(pac => pac.id == pacId)
        val poacs = tpacs.find(poac => poac.id == poacId)
         pacs.zip(poacs).map { case (pac, poac) => TPeriodicAccountBalance.transfer1(poac, pac, line.amount)}
        List(pacs, poacs).flatten
      })
    )
    r
  }*/

  private def makeJournal(model: FinancialsTransaction, pacList: List[PeriodicAccountBalance]): List[Journal] = for {
    journal <- model.lines.flatMap { line =>
                 val pac: Option[PeriodicAccountBalance]  = pacList.find(pac_ => pac_.id == buildPacId(model.getPeriod, line.account))
                 val poac: Option[PeriodicAccountBalance] = pacList.find(poac_ => poac_.id == buildPacId(model.getPeriod, line.oaccount))
                 val jou1                                 = pac.map(buildJournalEntries(model, line, _, line.account, line.oaccount,line.side))
                 val jou2                                 = poac.map(buildJournalEntries(model, line, _, line.oaccount, line.account,!line.side))
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
      // model.file_content,
      model.modelid
    )
}

object FinancialsServiceImpl {
  val live: ZLayer[PacRepository with TransactionRepository with JournalRepository, RepositoryError, FinancialsService] =
    ZLayer.fromFunction(new FinancialsServiceImpl(_, _, _))
}
