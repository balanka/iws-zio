package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain._
import com.kabasoft.iws.domain.common._
import com.kabasoft.iws.repository._
import zio._
import zio.prelude.FlipOps


final class PostBillOfDeliveryImpl(pacRepo: PacRepository, accRepo: AccountRepository, artRepo: ArticleRepository
                                  , stockRepo: StockRepository, repository4PostingTransaction:PostTransactionRepository)
                                    extends PostBillOfDelivery {

  override def postAll(transactions: List[Transaction], company:Company): ZIO[Any, RepositoryError, Int]  =
    for {
      _ <-ZIO.foreachDiscard(transactions.map(_.id)) (
        id =>ZIO.logInfo(s"Posting bill of delivery  transaction  with id ${id} of company ${transactions.head.company}"))
       stockIds = Stock.create(transactions).map(_.id).distinct
      oldStocks <-  stockRepo.getById(stockIds)
      post <-  postTransaction(transactions, company, Nil, oldStocks)
     nr <- repository4PostingTransaction.post(post._1, post._2, post._3, post._4, post._5, post._6, post._7, post._8)
    } yield nr

  private[this] def postTransaction(transactions: List[Transaction], company: Company, newStock:List[Stock], oldStocks:List[Stock]):
  ZIO[Any, RepositoryError, (List[Transaction], List[PeriodicAccountBalance], ZIO[Any, Nothing, List[PeriodicAccountBalance]],
                             List[TransactionLog], List[Journal], List[Stock], List[Stock], List[Article])] = for {

    accounts <- accRepo.all(Account.MODELID, company.id)
    articles <- artRepo.getBy(transactions.flatMap(m => m.lines.map(_.article)), company.id)
    accountIds = articles.map(art => (art.stockAccount, company.salesClearingAcc))
    pacids = accountIds.flatMap(id => transactions.map(tr => buildPacId(tr.period, id))).flatten
    pacs <- pacRepo.getByIds(pacids, company.id).map(_.filterNot(_.id.equals(PeriodicAccountBalance.dummy.id)))
    allPacs = transactions.flatMap(tr => buildPacsFromTransaction(tr, articles, accounts, company))
    newRecords = allPacs.filterNot(pac => pacs.map(_.id).contains(pac.id))
      .groupBy(_.id) map { case (_, v) => common.reduce(v, PeriodicAccountBalance.dummy) }
    tpacs <- pacs.map(TPeriodicAccountBalance.apply).flip
    oldPacs <- updatePac(allPacs, tpacs).map(e => e.map(PeriodicAccountBalance.applyT))
    journalEntries <- makeJournal(transactions, newRecords.toList, oldPacs, articles)
    stocks <- updateStock(transactions, oldStocks)
    transLogEntries <- buildTransactionLog(transactions, stocks, newStock, articles)

  } yield (transactions, newRecords.toList, oldPacs.flip,
      transLogEntries, journalEntries, stocks, newStock, Nil)

  private def filterIWS[A <: IWS](list: List[A], param: String): List[A] = list.filter(_.id == param)
  private def articleId2AccountId(articleId:String, articles:List[Article], accounts:List[Account]): List[String] =
    filterIWS(articles,  articleId).flatMap(art=>filterIWS(accounts, art.stockAccount).map(_.id))


  private[this] def buildPacsFromTransaction(model:Transaction, articles:List[Article], accounts:List[Account], company: Company): List[PeriodicAccountBalance] =
    model.lines.flatMap { line =>
      val stockAccountIds: List[String] = articleId2AccountId(line.article, articles, accounts)
     stockAccountIds.map( accountId=>createPac (accountId, model, line))++List(createPac (company.salesClearingAcc, model, line))
  }

  private def createPac (accountId:String, model:Transaction, line:TransactionDetails) =
    PeriodicAccountBalance.apply(
      PeriodicAccountBalance.createId(model.period, accountId),
      accountId,
      model.period,
      zeroAmount,
      zeroAmount,
      line.quantity.multiply(line.price),
      zeroAmount,
      line.currency,
      model.company,
      "",
      PeriodicAccountBalance.MODELID
    )
  private[this] def buildPacId(period: Int, accountId:(String,  String)): List[String] =
    List(PeriodicAccountBalance.createId(period, accountId._1), PeriodicAccountBalance.createId(period, accountId._2))

  private[this] def buildPacId2(period: Int, accountId:(String,  String)): (String, String) =
    (PeriodicAccountBalance.createId(period, accountId._1), PeriodicAccountBalance.createId(period, accountId._2))

  private def updateStock(transactions: List[Transaction], oldStocks:List[Stock]): ZIO[Any, Nothing, List[Stock]] = for{
      updatedStock <- updateOldStock(Stock.create(transactions).map(stock =>stock.copy(quantity = stock.quantity.negate())), oldStocks)
        .map(_.map(Stock.apply).flip).flatten
    }yield   updatedStock

  private def updateOldStock(stocksNew:List[Stock], oldStocks:List[Stock]): ZIO[Any, Nothing, List[TStock]] = for {
    updatedStock <- stocksNew
      .flatMap(ts=> oldStocks.filter(st=>st.id==ts.id)
      .map(st=>TStock.apply(st, ts.quantity))).flip
  }yield updatedStock

  private def updatePac(oldPacs: List[PeriodicAccountBalance], tpacs: List[TPeriodicAccountBalance]): ZIO[Any, Nothing, List[TPeriodicAccountBalance]] =
  for{
    newRecords<- groupById(oldPacs).map(TPeriodicAccountBalance.apply).flip
             _<- newRecords.map ( pac =>transfer(pac, tpacs)).flip
  }yield ( if(tpacs.nonEmpty) tpacs  else newRecords)

  private def transfer( pac:TPeriodicAccountBalance,  tpacs:List[TPeriodicAccountBalance]): ZIO[Any, Nothing, Option[Unit]] =
    tpacs.find(_.id ==  pac.id).map(pac_ => pac_.transfer(pac, pac_)).flip

  private def groupById(r: List[PeriodicAccountBalance]) =
  (r.groupBy(_.id) map { case (_, v) =>
      common.reduce(v, PeriodicAccountBalance.dummy)
    }).toList


  def findOrBuildPac(pacId: String, period_ : Int, pacList: List[PeriodicAccountBalance]): PeriodicAccountBalance =
    pacList.find(pac_ => pac_.id == pacId).getOrElse(PeriodicAccountBalance.dummy.copy(id = pacId, period = period_))

  private def makeJournal(models: List[Transaction],  pacListx: List[PeriodicAccountBalance], tpacList: List[UIO[PeriodicAccountBalance]],
                          articles:List[Article] ): ZIO[Any, Nothing, List[Journal]] = for {
      pacList <- tpacList.flip
      journal = models.flatMap(model =>
        if (model.modelid == TransactionModelId.GOORECEIVING.id) {
          model.lines.flatMap { line =>
            val accountId = articles.filter(_.id == line.article).map(art => (art.stockAccount, art.expenseAccount)).head
            val pacId = buildPacId2(model.getPeriod, (accountId._1, accountId._2))
            val pac = findOrBuildPac(pacId._1, model.getPeriod, pacList ++ pacListx)
            val poac = findOrBuildPac(pacId._2, model.getPeriod, pacList ++ pacListx)
            val jou1 = buildJournalEntries(model, line, pac, pacId._1, pacId._2, side = true)
            val jou2 = buildJournalEntries(model, line, poac, pacId._2, pacId._1, side = false)
            List(jou1, jou2)
          }
        } else List.empty[Journal])
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

  private def buildTransactionLog(models: List[Transaction], stocks:List[Stock], newStock: List[Stock],
                          articles:List[Article] ): ZIO[Any, Nothing, List[TransactionLog]] = ZIO.succeed {
    val allStock = stocks++newStock
    models.flatMap(tr => tr.lines.map( line =>
      allStock.find(_.id == tr.store.concat(line.article).concat(tr.company).concat("")) // Find stock for article  in  store
      .flatMap(st=> articles.find(_.id == st.article).map( article =>                    // Find article for the line
     TransactionLog(0L, tr.id, tr.oid, tr.store, tr.costcenter, line.article, line.quantity // build TransactionLog
       , st.quantity, /*article.wholeStock*/ zeroAmount, article.quantityUnit , article.pprice , article.avgPrice
       , article.currency, line.duedate, line.text, tr.transdate, tr.postingdate, tr.enterdate, tr.period, tr.company, tr.modelid)
      ))).map(_.toList)).flatten
  }
}

object PostBillOfDeliveryImpl {
  val live: ZLayer[PacRepository with TransactionRepository with TransactionLogRepository with AccountRepository
    with ArticleRepository with StockRepository with PostTransactionRepository, RepositoryError, PostBillOfDelivery] =
    ZLayer.fromFunction(new PostBillOfDeliveryImpl(_, _, _, _, _))
}


