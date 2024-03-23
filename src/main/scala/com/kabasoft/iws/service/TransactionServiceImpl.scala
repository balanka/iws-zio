package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.common._
import com.kabasoft.iws.domain._
import com.kabasoft.iws.repository.{AccountRepository, ArticleRepository, JournalRepository, PacRepository, PostTransactionRepository, StockRepository, TransactionRepository}
import zio._
import zio.prelude.FlipOps

import java.math.RoundingMode
import scala.collection.immutable

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
      nr <- ZIO.foreach(models)(trans => postTransaction(List(trans), company, Nil, Nil)).map(_.toList).map(_.sum)
    } yield nr

  override def postAll(ids: List[Long], company: String): ZIO[Any, RepositoryError, Int] =
    for {
      queries <- ZIO.foreach(ids)(id => ftrRepo.getByTransId((id, company)))
      models = queries.filter(_.posted == false)
      _ <-ZIO.foreachDiscard(models.map(_.id)) (
        id =>ZIO.logInfo(s"Posting transaction with id ${id} of company ${company}"))
       stockIds = Stock.create(models).map(_.id).distinct
      oldStocks <-  stockRepo.getById(stockIds)
      newStock <-  buildNewStock(models, oldStocks ).flip
      nr <-  postTransaction(models, company, newStock, oldStocks)
    } yield nr

  override def post(id: Long, company: String): ZIO[Any, RepositoryError, Int] =
    ZIO.logInfo(s" Posting transaction with id ${id} of company ${company}") *>
      ftrRepo
        .getByTransId((id, company))
        .flatMap(trans => postTransaction(List(trans), company, Nil, Nil))

  private[this] def postTransaction(transactions: List[Transaction], company: String, newStock:List[Stock], oldStocks:List[Stock]): ZIO[Any, RepositoryError, Int] = {
    for {
      accounts <- accRepo.all(Account.MODELID, company)
      articles <- artRepo.getBy(transactions.flatMap(m=>m.lines.map(_.article)), company)
      accountIds = articles.map(art=>(art.stockAccount, art.expenseAccount))
      pacids = accountIds.flatMap(id => transactions.map(tr=>buildPacId(tr.period, id))).flatten
      pacs <- pacRepo.getByIds(pacids, company).map(_.filterNot(_.id.equals(PeriodicAccountBalance.dummy.id)))
      allPacsx = transactions.flatMap(tr =>buildPacsFromTransaction(tr, articles, accounts))
      newRecords = allPacsx.filterNot(pac => pacs.map(_.id).contains(pac.id))
        .groupBy(_.id) map { case (_, v) => common.reduce(v, PeriodicAccountBalance.dummy)}
      tpacs <- pacs.map(TPeriodicAccountBalance.apply).flip
      oldPacs <- updatePac(allPacsx, tpacs).map(e=>e.map(PeriodicAccountBalance.applyT))
      journalEntries <- makeJournal(transactions, newRecords.toList, oldPacs, articles)
      stocks <- updateStock(transactions, oldStocks)
      updatedArticle <- updateAvgPrice(transactions, stocks, articles)
      post <- repository4PostingTransaction.post(transactions, newRecords.toList, oldPacs.flip, journalEntries, stocks, newStock, updatedArticle)

    } yield post
  }

//  private[this] def postTransaction(transaction: Transaction, company: String, newStock:List[Stock], oldStocks:List[Stock]): ZIO[Any, RepositoryError, Int] = {
//    val model = transaction.copy(period = common.getPeriod(transaction.transdate))
//    for {
//      accounts <- accRepo.all(Account.MODELID, company)
//      articles <- artRepo.getBy(model.lines.map(_.article), company)
//      accountIds = articles.map(art=>(art.stockAccount, art.expenseAccount))
//      pacids = accountIds.flatMap(id => buildPacId(model.period, id))
//      pacs <- pacRepo.getByIds(pacids, company).map(_.filterNot(_.id.equals(PeriodicAccountBalance.dummy.id)))
//      allPacsx = buildPacsFromTransaction(model, articles, accounts)
//      newRecords = allPacsx.filterNot(pac => pacs.map(_.id).contains(pac.id))
//        .groupBy(_.id) map { case (_, v) => common.reduce(v, PeriodicAccountBalance.dummy)}
//      tpacs <- pacs.map(TPeriodicAccountBalance.apply).flip
//      oldPacs <- updatePac(allPacsx, tpacs).map(e=>e.map(PeriodicAccountBalance.applyT))
//      journalEntries <- makeJournal(model, newRecords.toList, oldPacs, articles, accounts)
//      stocks <- updateStock(model, oldStocks)
//      updatedArticle <- updateAvgPrice(model, stocks._1++stocks._2, articles)
//      post <- repository4PostingTransaction.post(List(model), newRecords.toList, oldPacs.flip, journalEntries, stocks, newStock, updatedArticle.flatten)
//
//    } yield post
//  }
 
  private def articleId2AccountId(articleId:String, articles:List[Article], accounts:List[Account]): (List[String], List[String]) =
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

  private[this] def buildPacId2(period: Int, accountId:(String,  String)): (String, String) =
    (PeriodicAccountBalance.createId(period, accountId._1), PeriodicAccountBalance.createId(period, accountId._2))
  private def updateStock(transactions: List[Transaction], oldStocks:List[Stock]): ZIO[Any, Nothing, List[Stock]] = for{
      updatedStock <- updateOldStock(transactions, oldStocks).map(_.map(Stock.apply).flip).flatten
    }yield   updatedStock

  private def updateArticleAvgPrice(line: TransactionDetails, stocks:List[Stock], articles:List[Article]): ZIO[Any, Nothing, List[Article]] =
    articles.map ( article => {
    val purchasedValue = line.price.multiply(line.quantity)
    val wholeStock = groupByStockFirst(stocks.filter(st=>st.article == line.article))
    val wholeQuantityBefore = wholeStock.fold(zeroAmount)(_.quantity)
    val wholeValueBefore = wholeQuantityBefore.multiply(article.avgPrice)
    val wholeValueAfter = wholeValueBefore.add(purchasedValue)
    val wholeQuantityAfter = wholeQuantityBefore.add(line.quantity)
    val avgPriceAfter = if(wholeQuantityAfter.compareTo(zeroAmount)>0) wholeValueAfter.divide(wholeQuantityAfter, 2, RoundingMode.HALF_UP)  else line.price
    val avgPrice_ = wholeStock.fold(line.price)(_ =>avgPriceAfter)
      ZIO.succeed(article.copy(avgPrice = avgPrice_))
  }).flip
  private def updateAvgPrice(transactions: List[Transaction], stocks:List[Stock], articles:List[Article]): ZIO[Any, Nothing, List[Article]] = {
    val x = transactions.flatMap(tr => tr.lines.map(line => updateArticleAvgPrice( line,  stocks, articles.filter(_.id == line.article).distinct)))
    x.flip.map(_.flatten)
  }

  private def buildNewStock(transactions: List[Transaction], stocks:List[Stock]) = for {
    newRecords <-Stock.create(transactions).filterNot(stock=>stocks.map(_.id).contains(stock.id))
  }yield ZIO.succeed(newRecords)
  private def updateOldStock(transactions: List[Transaction], oldStocks:List[Stock]): ZIO[Any, Nothing, List[TStock]] = for {
    updatedStock <-groupByStock(Stock.create(transactions))
      .flatMap(ts=> oldStocks.filter(st=>st.id==ts.id)
      .map(st=>TStock.apply(st, ts.quantity))).flip
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

  private def groupById(r: List[PeriodicAccountBalance]) =
  (r.groupBy(_.id) map { case (_, v) =>
      common.reduce(v, PeriodicAccountBalance.dummy)
    }).toList

  private def groupByStockFirst(r: List[Stock]) =
    (r.groupBy(_.article) map { case (_, v) =>
      common.reduce(v, Stock.dummy)
    }).filterNot(_.article == Stock.dummy.article).headOption
  private def groupByStock(r: List[Stock]) =
    (r.groupBy(_.article) map { case (_, v) =>
      common.reduce(v, Stock.dummy)
    }).filterNot(_.article == Stock.dummy.article).toList

  def findOrBuildPac(pacId: String, period_ : Int, pacList: List[PeriodicAccountBalance]): PeriodicAccountBalance =
    pacList.find(pac_ => pac_.id == pacId).getOrElse(PeriodicAccountBalance.dummy.copy(id = pacId, period = period_))

  private def makeJournal(models: List[Transaction],  pacListx: List[PeriodicAccountBalance], tpacList: List[UIO[PeriodicAccountBalance]],
                          articles:List[Article] ): ZIO[Any, Nothing, List[Journal]] =
    for {
    pacList<-tpacList.flip
    journal = models.flatMap(model=>model.lines.flatMap { line =>
     val accountId = articles.filter(_.id == line.article).map(art=>(art.stockAccount, art.expenseAccount)).head
      val pacId = buildPacId2(model.getPeriod, (accountId._1, accountId._2))
      val pac = findOrBuildPac(pacId._1, model.getPeriod, pacList++pacListx)
      val poac = findOrBuildPac(pacId._2, model.getPeriod, pacList++pacListx)
      val jou1 = buildJournalEntries(model, line, pac, pacId._1, pacId._2, side = true)
      val jou2 = buildJournalEntries(model, line, poac, pacId._2, pacId._1, side = false)
      List(jou1, jou2)
    })
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
