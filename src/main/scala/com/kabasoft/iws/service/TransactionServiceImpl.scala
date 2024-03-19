package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.common._
import com.kabasoft.iws.domain._
import com.kabasoft.iws.repository.{AccountRepository, ArticleRepository, FinancialsTransactionRepository, JournalRepository, PacRepository, PostTransactionRepository, StockRepository, TransactionRepository}
import zio._
import zio.prelude.FlipOps

final class TransactionServiceImpl(pacRepo: PacRepository, ftrRepo: TransactionRepository, journalRepo: JournalRepository,
            accRepo: AccountRepository,  artRepo: ArticleRepository, stockRepo: StockRepository,
                                   repository4PostingTransaction:PostTransactionRepository) extends TransactionService {

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

  private[this] def postTransaction(transaction: Transaction, company: String): ZIO[Any, RepositoryError, Int] = {
    val model = transaction.copy(period = common.getPeriod(transaction.transdate))
    for {
      accounts <- accRepo.all(Account.MODELID, company)

      accountIds <- artRepo.getBy(transaction.lines.map(_.article), company).map(_.map(art=>(art.stockAccount, art.expenseAccount)))
      pacids = accountIds.flatMap(id => buildPacId(model.period, id))
      pacs <- pacRepo.getByIds(pacids, company).map(_.filterNot(_.id.equals(PeriodicAccountBalance.dummy.id)))
      articles <- artRepo.getBy(transaction.lines.map(_.article), company)
      allPacsx = buildPacsFromTransaction(model, articles, accounts)
      newRecords = allPacsx.filterNot(pac => pacs.map(_.id).contains(pac.id))
        .groupBy(_.id) map { case (_, v) => common.reduce(v, PeriodicAccountBalance.dummy)}
      tpacs <- pacs.map(TPeriodicAccountBalance.apply).flip
      oldPacs <- updatePac(allPacsx, tpacs).map(e=>e.map(PeriodicAccountBalance.applyT))
      journalEntries <- makeJournal(model, newRecords.toList, oldPacs, articles, accounts)
      stocks <- updateStock(transaction)
      post <- repository4PostingTransaction.post(List(model), newRecords.toList, oldPacs.flip, journalEntries, stocks)

    } yield post
  }
 
  def articleId2AccountId( articleId:String, articles:List[Article], accounts:List[Account]): (List[String], List[String]) =
    (articles.filter(_.id == articleId).flatMap(art=>accounts.filter(_.id == art.stockAccount).map(_.id)),
    articles.filter(_.id == articleId).flatMap(art=>accounts.filter(_.id == art.expenseAccount).map(_.id)))

  def articleId2AccountId2( articleId:String, articles:List[Article], accounts:List[Account]): (String, String) =
    (articles.filter(_.id == articleId).flatMap(art=>accounts.filter(_.id == art.stockAccount).map(_.id)).head,
      articles.filter(_.id == articleId).flatMap(art=>accounts.filter(_.id == art.expenseAccount).map(_.id)).head)
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

  private[this] def buildPacId2(period: Int, accountId:(String,  String)): (String, String) =
    (PeriodicAccountBalance.createId(period, accountId._1), PeriodicAccountBalance.createId(period, accountId._2))
  private def updateStock(transaction: Transaction) = {
    val result = for{
      oldStocks <- transaction.lines.map(line => stockRepo.getBy (transaction.store, line.article, transaction.company)).flip//.map(_.map( x=>x.add(line.quantity))
      oldStockIds = oldStocks.map( st=> st.store.concat(st.article).concat(st.company))
      _ <- updateOldStock(transaction, oldStocks)
      newRecords <-buildStock(transaction, oldStockIds).flip
    }yield   (oldStocks, newRecords)
    result
  }
  private def buildStock(transaction: Transaction, oldStockIds:List[String]) = for {
    newRecords <-Stock.create(transaction).filterNot(stock=>oldStockIds.contains( (stock.store.concat(stock.article).concat(stock.company))))
  }yield ZIO.succeed(newRecords)

  private def updateOldStock(transaction: Transaction, oldStocks:List[Stock]) = for {
    oldTStocks <- oldStocks.map( Stock(_)).flip
    updatedStock <-transaction.lines.flatMap( line=>(oldTStocks.find(st =>
      (st.store == line.article && st.article == line.article && st.company == transaction.company))).map(l=>l.add(line.quantity))).flip
  }yield updatedStock

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
    }).toList
    r0
  }

  def findOrBuildPac(pacId: String, period_ : Int, pacList: List[PeriodicAccountBalance]): PeriodicAccountBalance =
    pacList.find(pac_ => pac_.id == pacId).getOrElse(PeriodicAccountBalance.dummy.copy(id = pacId, period = period_))

  private def makeJournal(model: Transaction,  pacListx: List[PeriodicAccountBalance], tpacList: List[UIO[PeriodicAccountBalance]],
                          articles:List[Article], accounts:List[Account] ): ZIO[Any, Nothing, List[Journal]] =
    for {
    pacList<-tpacList.flip
    journal = model.lines.flatMap { line =>
     val accountId = articleId2AccountId2( line.article,  articles:List[Article], accounts:List[Account])
      val pacId = buildPacId2(model.getPeriod, (accountId._1, accountId._2))
      val pac = findOrBuildPac(pacId._1, model.getPeriod, pacList++pacListx)
      val poac = findOrBuildPac(pacId._2, model.getPeriod, pacList++pacListx)
      val jou1 = buildJournalEntries(model, line, pac, pacId._1, pacId._2, side = true)
      val jou2 = buildJournalEntries(model, line, poac, pacId._2, pacId._1, side = false)
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
}

object TransactionServiceImpl {
  val live: ZLayer[PacRepository with TransactionRepository with JournalRepository with AccountRepository
    with ArticleRepository with StockRepository with PostTransactionRepository, RepositoryError, TransactionService] =
    ZLayer.fromFunction(new TransactionServiceImpl(_, _, _, _, _, _, _))
}
