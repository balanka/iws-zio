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
      _ <- ZIO.foreachDiscard(transactions.map(_.id))(
        id => ZIO.logInfo(s"Posting bill of delivery  transaction  with id ${id} of company ${transactions.head.company}"))
      stockIds = Stock.create(transactions).map(_.id).distinct
      oldStocks <- stockRepo.getById(stockIds)
      post <- postTransaction(transactions, company, Nil, oldStocks)
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
    allPacs = transactions.flatMap(tr => buildPacsFromTransaction(tr, articles, accounts, company.salesClearingAcc))
    newRecords = allPacs.filterNot(pac => pacs.map(_.id).contains(pac.id))
      .groupBy(_.id) map { case (_, v) => common.reduce(v, PeriodicAccountBalance.dummy) }
    tpacs <- pacs.map(TPeriodicAccountBalance.apply).flip
    oldPacs <- updatePac(allPacs, tpacs).map(e => e.map(PeriodicAccountBalance.applyT))
    journalEntries <- makeJournal(transactions, newRecords.toList, oldPacs, articles)
    stocks <- updateStock(transactions, oldStocks)
    transLogEntries <- buildTransactionLog(transactions, stocks, newStock, articles)

  } yield (transactions, newRecords.toList, oldPacs.flip, transLogEntries, journalEntries, stocks, newStock, Nil)

  private def updateStock(transactions: List[Transaction], oldStocks:List[Stock]): ZIO[Any, Nothing, List[Stock]] = for{
      updatedStock <- updateOldStock(Stock.create(transactions).map(stock =>stock.copy(quantity = stock.quantity.negate())), oldStocks)
        .map(_.map(Stock.apply).flip).flatten
    }yield   updatedStock

  private def updateOldStock(stocksNew:List[Stock], oldStocks:List[Stock]): ZIO[Any, Nothing, List[TStock]] = for {
    updatedStock <- stocksNew
      .flatMap(ts=> oldStocks.filter(st=>st.id==ts.id)
      .map(st=>TStock.apply(st, ts.quantity))).flip
  }yield updatedStock

  private def makeJournal(models: List[Transaction],  pacListx: List[PeriodicAccountBalance], tpacList: List[UIO[PeriodicAccountBalance]],
                          articles:List[Article] ): ZIO[Any, Nothing, List[Journal]] = for {
      pacList <- tpacList.flip
      journal = models.flatMap(model =>
          model.lines.flatMap { line =>
            val accountId = articles.filter(_.id == line.article).map(art => (art.stockAccount, art.expenseAccount)).head
            val pacId = buildPacId2(model.getPeriod, (accountId._1, accountId._2))
            val pac = findOrBuildPac(pacId._1, model.getPeriod, pacList ++ pacListx)
            val poac = findOrBuildPac(pacId._2, model.getPeriod, pacList ++ pacListx)
            val jou1 = buildJournalEntries(model, line, pac, pacId._1, pacId._2, side = true)
            val jou2 = buildJournalEntries(model, line, poac, pacId._2, pacId._1, side = false)
            List(jou1, jou2)
          })
    } yield journal

}

object PostBillOfDeliveryImpl {
  val live: ZLayer[PacRepository with TransactionRepository with TransactionLogRepository with AccountRepository
    with ArticleRepository with StockRepository with PostTransactionRepository, RepositoryError, PostBillOfDelivery] =
    ZLayer.fromFunction(new PostBillOfDeliveryImpl(_, _, _, _, _))
}