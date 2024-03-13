package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.common._
import com.kabasoft.iws.domain._
import com.kabasoft.iws.repository.{AccountRepository, ArticleRepository, FinancialsTransactionRepository, JournalRepository, PacRepository, PostTransactionRepository, TransactionRepository}
import zio._
import zio.prelude.FlipOps

final class TransactionServiceImpl(pacRepo: PacRepository, ftrRepo: TransactionRepository, journalRepo: JournalRepository,
            accRepo: AccountRepository,  artRepo: ArticleRepository, repository4PostingTransaction:PostTransactionRepository) extends TransactionService {

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
      _ <-ZIO.foreachDiscard(models.map(_.id)) (
        id =>ZIO.logInfo(s"Posting transaction with id ${id} of company ${company}"))
      nr <- ZIO.foreach(models)(model => postTransaction(model, company)).map(_.sum)
    } yield nr

  override def post(id: Long, company: String): ZIO[Any, RepositoryError, Int] =
    ZIO.logInfo(s" Posting transaction with id ${id} of company ${company}") *>
      ftrRepo
        .getByTransId((id, company))
        .flatMap(trans => postTransaction(trans, company))

  private[this] def postTransaction(transaction: Transaction, company: String): ZIO[Any, RepositoryError, Int] =
    /*{
  val model = transaction.copy(period = common.getPeriod(transaction.transdate))
  val pacids = buildPacIds(model)
  for {

    pacs <- pacRepo.getByIds(pacids, company).map(_.filterNot(_.id.equals(PeriodicAccountBalance.dummy.id)))
    newRecords = PeriodicAccountBalance.create(model).filterNot(pac => pacs.map(_.id).contains(pac.id))
      .groupBy(_.id) map { case (_, v) => common.reduce(v, PeriodicAccountBalance.dummy)}
    tpacs <- pacs.map(TPeriodicAccountBalance.apply).flip
    oldPacs <- updatePac(model, tpacs).map(e=>e.map(PeriodicAccountBalance.applyT))
    journalEntries <- makeJournal(model, newRecords.toList, oldPacs)
    post <- repository4PostingTransaction.post(List(model), newRecords.toList, oldPacs.flip, journalEntries)

  } yield post
}
 */
  {
    val model = transaction.copy(period = common.getPeriod(transaction.transdate))
    //val pacids = buildPacIds(model)
    for {
      //
      accounts <- accRepo.all(Account.MODELID, company)
      accountIds <- artRepo.getBy(transaction.lines.map(_.article), company).map(x=>x.map(art=>(art.stockAccount, art.expenseAccount)))
      pacids = accountIds.flatMap(id => buildPacId(model.period, id))
      pacs <- pacRepo.getByIds(pacids, company).map(_.filterNot(_.id.equals(PeriodicAccountBalance.dummy.id)))
      articles <- artRepo.getBy(transaction.lines.map(_.article), company)
      allPacsx = buildPacsFromTransaction(model, articles, accounts)
      newRecords = allPacsx.filterNot(pac => pacs.map(_.id).contains(pac.id))
        .groupBy(_.id) map { case (_, v) => common.reduce(v, PeriodicAccountBalance.dummy)}
      tpacs <- pacs.map(TPeriodicAccountBalance.apply).flip
      oldPacs <- updatePac(allPacsx, tpacs).map(e=>e.map(PeriodicAccountBalance.applyT))
      journalEntries <- makeJournal(model, newRecords.toList, oldPacs)
      post <- repository4PostingTransaction.post(List(model), newRecords.toList, oldPacs.flip, journalEntries)

    } yield post
 }
  def articleId2AccountId( articleId:String, articles:List[Article], accounts:List[Account]): (List[String], List[String]) =
    (articles.filter(_.id == articleId).flatMap(art=>accounts.filter(_.id == art.stockAccount).map(_.id)),
    articles.filter(_.id == articleId).flatMap(art=>accounts.filter(_.id == art.expenseAccount).map(_.id)))

  private[this] def buildPacsFromTransaction(model:Transaction, articles:List[Article], accounts:List[Account]) =
    model.lines.flatMap { line =>
      val (stockAccount, expenseAccount) = articleId2AccountId(line.article, articles, accounts)
    List(
      stockAccount.map( acc=>PeriodicAccountBalance.apply(
        PeriodicAccountBalance.createId(model.period, acc),
        acc,
        model.period,
        zeroAmount,
        zeroAmount,
        line.quantity.multiply(line.price),
        zeroAmount,
        line.currency,
        model.company,
        "",
        PeriodicAccountBalance.MODELID
      )),
      expenseAccount.map( acc=>
      PeriodicAccountBalance.apply(
        PeriodicAccountBalance.createId(model.period, acc),
        acc,
        model.period,
        zeroAmount,
        zeroAmount,
        zeroAmount,
        line.quantity.multiply(line.price),
        line.currency,
        model.company,
        "",
        PeriodicAccountBalance.MODELID
      )
    )
    ).flatten
  }


  private[this] def buildPacId(period: Int, accountId:(String,  String)): List[String] =
    List(PeriodicAccountBalance.createId(period, accountId._1), PeriodicAccountBalance.createId(period, accountId._2))


  private def updatePac(oldPacs: List[PeriodicAccountBalance], tpacs: List[TPeriodicAccountBalance]): ZIO[Any, Nothing, List[TPeriodicAccountBalance]] = {
  val result = for{
    newRecords<- groupById(oldPacs).map(TPeriodicAccountBalance.apply).flip
             _<- newRecords.map ( pac =>transfer(pac, tpacs)).flip
  }yield ( if(tpacs.nonEmpty) tpacs  else newRecords)
  result
}

  private def transfer( pac:TPeriodicAccountBalance,  tpacs:List[TPeriodicAccountBalance]): ZIO[Any, Nothing, Option[Unit]] =
    tpacs.find(_.id ==  pac.id).map(pac_ => pac_.transfer(pac, pac_)).flip

  private def groupById(r: List[PeriodicAccountBalance]) = {
   val r0 = (r.groupBy(_.id) map { case (_, v) =>
      common.reduce(v, PeriodicAccountBalance.dummy)
    }).toList//.filterNot(_.id == PeriodicAccountBalance.dummy.id)
    r0
  }



  def findOrBuildPac(pacId: String, period_ : Int, pacList: List[PeriodicAccountBalance]) =
    pacList.find(pac_ => pac_.id == pacId).getOrElse(PeriodicAccountBalance.dummy.copy(id = pacId, period = period_))


  private def makeJournal(model: Transaction,  pacListx: List[PeriodicAccountBalance], tpacList: List[UIO[PeriodicAccountBalance]]): ZIO[Any, Nothing, List[Journal]] =
    ZIO.succeed(List.empty[Journal])
  /*  for {
    pacList<-tpacList.flip
    journal = model.lines.flatMap { line =>
      val pacId = buildPacId(model.getPeriod, line.account)
      val poacId = buildPacId(model.getPeriod, line.oaccount)
      val pac = findOrBuildPac(pacId, model.getPeriod, pacList++pacListx)
      val poac = findOrBuildPac(poacId, model.getPeriod, pacList++pacListx)
      val jou1 = buildJournalEntries(model, line, pac, line.account, line.oaccount, line.side)
      val jou2 = buildJournalEntries(model, line, poac, line.oaccount, line.account, !line.side)
      List(jou1, jou2)
    }
  } yield journal
  private def buildJournalEntries(model: Transaction, line: TransactionDetails,
                                  pac: PeriodicAccountBalance, account: String, oaccount: String,side:Boolean) =
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
      line.quantity.multiply(line.price),
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

   */
}

object TransactionServiceImpl {
  val live: ZLayer[PacRepository with TransactionRepository with JournalRepository with AccountRepository
    with ArticleRepository with PostTransactionRepository, RepositoryError, TransactionService] =
    ZLayer.fromFunction(new TransactionServiceImpl(_, _, _, _, _, _))
}