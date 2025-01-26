package com.kabasoft.iws.service

import com.kabasoft.iws.config.appConfig
import com.kabasoft.iws.domain.AccountBuilder.companyId
import com.kabasoft.iws.domain.ArticleBuilder.{artId0, artId1, articleList}
import com.kabasoft.iws.domain.TransactionBuilder.{ftr1, ftr2, ftr3, ftr4, ftr5, ftr6}
import com.kabasoft.iws.domain.{Article, Stock, Transaction}
import com.kabasoft.iws.repository.container.PostgresContainer
import com.kabasoft.iws.repository.*
import com.kabasoft.iws.repository.container.PostgresContainer.appResourcesL
import zio.ZLayer
import zio.prelude.FlipOps
import zio.test.TestAspect.*
import zio.test.*

import java.math.{BigDecimal, RoundingMode}

object TransactionServiceLiveSpec extends ZIOSpecDefault {
  val DOUBLE =  new BigDecimal("2.00").setScale(2, RoundingMode.HALF_UP)
  def doublePrice (tr: Transaction): Transaction = tr.copy( lines = tr.lines.map(l=>l.copy(price = l.price.multiply(DOUBLE))))
    .copy(id1= -1)
  val testServiceLayer = ZLayer.make[AccountService& TransactionService& TransactionRepository & PostOrder
    & PostSalesOrder& ArticleRepository& AccountRepository& PacRepository& StockRepository & CustomerRepository
    &  SupplierRepository&  VatRepository& PostGoodreceiving& PostBillOfDelivery &  PostCustomerInvoice
    & TransactionLogRepository &  PostSupplierInvoice& PostFinancialsTransactionRepository](
    appResourcesL.project(_.postgres),
    appConfig,
    AccountRepositoryLive.live,
    AccountServiceLive.live,
    BankAccountRepositoryLive.live,
    //MasterfileRepositoryLive.live,
    ArticleRepositoryLive.live,
    CustomerRepositoryLive.live,
    SupplierRepositoryLive.live,
    CompanyRepositoryLive.live,
    StockRepositoryLive.live,
    VatRepositoryLive.live,
    PacRepositoryLive.live,
    JournalRepositoryLive.live,
    TransactionRepositoryLive.live,
    TransactionLogRepositoryLive.live,
    TransactionServiceLive.live,
    PostTransactionRepositoryLive.live,
    PostFinancialsTransactionRepositoryLive.live,
    PostOrderLive.live,
    PostSalesOrderLive.live,
    PostGoodreceivingLive.live,
    PostBillOfDeliveryLive.live,
    PostCustomerInvoiceLive.live,
    PostSupplierInvoiceLive.live,
    //PostgresContainer.createContainer
  )
  val list  = List(ftr1, ftr2, ftr3, ftr4, ftr5, ftr6)
  val createdStock = Stock.create(list)
  val stock0 = Stock.buildId(ftr1.store, artId0, "", ftr1.company)
  val stock1 = Stock.buildId(ftr1.store, artId1, "", ftr1.company)
  val ids = createdStock.map(_.id)
  val articleids = articleList.map(_.id)
  override def spec =
    suite("TransactionService  test with postgres test container")(
      test("clear logistical all transactions ") {
        for
          deletedTransactions <- TransactionRepository.deleteAll()
          deletedPac <- PacRepository.deleteAll()
          deletedStock <- StockRepository.deleteAll()
          deletedArticle <- ArticleRepository.deleteAll(articleids, Article.MODELID, companyId)
          deletedTransactionLog <- TransactionLogRepository.deleteAll()
        yield assertTrue(deletedTransactions == 1) &&
          assertTrue(deletedPac ==1) &&
          assertTrue(deletedStock ==1) &&
          assertTrue(deletedArticle == articleList.size) &&
          assertTrue(deletedTransactionLog == 1)
      },
      test("create a set of  logistical transaction") {
        for 
          oneRow     <- TransactionRepository.create(list)
          createdArticle     <- ArticleRepository.create(articleList)
         yield assertTrue(oneRow ==  list.size+list.flatMap(_.lines).size ) &&
           assertTrue(createdArticle ==  articleList.size)
      },
      test("search, find  and post some logistical transaction meeting some criteria") {
        for 
          all        <-   TransactionRepository.all(ftr1.modelid, companyId)
          transactionIds:List[(Long, Int)] = all.map(tr =>(tr.id1, tr.modelid))
          postedRows <- TransactionService.postAll(transactionIds, ftr1.company)
        yield assertTrue(postedRows == 3)
      },
      test("search, find the stock of some  item and check the purchase and average price") {
        val avgPrice0 =  new BigDecimal("10.00").setScale(2, RoundingMode.HALF_UP)
        val avgPrice1 =  new BigDecimal("50.00").setScale(2, RoundingMode.HALF_UP)
        val pPrice0 =  new BigDecimal("10.00").setScale(2, RoundingMode.HALF_UP)
        val pPrice1 =  new BigDecimal("50.00").setScale(2, RoundingMode.HALF_UP)
        val  quantityStock0=  new BigDecimal("100.00").setScale(2, RoundingMode.HALF_UP)
        val  quantityStock1=  new BigDecimal("100.00").setScale(2, RoundingMode.HALF_UP)
        val stock0 = Stock.buildId(ftr1.store, artId0, "", ftr1.company)
        val stock1 = Stock.buildId(ftr1.store, artId1, "", ftr1.company)
        for 
          stocks <- StockRepository.getBy(createdStock.map(stock=>stock.id), Stock.MODELID, ftr1.company)
          stock0 <- StockRepository.getById (stock0, Stock.MODELID, ftr1.company)//.debug(s"stock0 >>> ${artId0}")
          stock1 <- StockRepository.getById (stock1, Stock.MODELID, ftr1.company)//.debug(s"stock1 >>> ${artId1}")
          article0 <- ArticleRepository.getById ( artId0, Article.MODELID, ftr1.company)//.debug(s"article0 >>> ${artId0}")
          article1 <- ArticleRepository.getById ( artId1, Article.MODELID, ftr1.company)//.debug(s"article1 >>> ${artId1}")
        yield assertTrue(stocks.size == 2, stock0.quantity.compareTo(quantityStock0)==0
            ,  stock1.quantity.compareTo(quantityStock1)==0
            , article0.avgPrice.compareTo(avgPrice0)==0
            , article0.pprice.compareTo(pPrice0)==0
            , article1.avgPrice.compareTo(avgPrice1)==0
            , article1.pprice.compareTo(pPrice1)==0)
      }
      ,
      test("create additional transaction, search, find the stock of some  item and check the purchase and average price") {
        val avgPrice0 =  new BigDecimal("13.33").setScale(2, RoundingMode.HALF_UP)
        val avgPrice1 =  new BigDecimal("66.67").setScale(2, RoundingMode.HALF_UP)
        val pPrice0 =  new BigDecimal("10.00").setScale(2, RoundingMode.HALF_UP).multiply(DOUBLE)
        val pPrice1 =  new BigDecimal("50.00").setScale(2, RoundingMode.HALF_UP).multiply(DOUBLE)
        val  quantityStock0=  new BigDecimal("200.00").setScale(2, RoundingMode.HALF_UP)
        val  quantityStock1=  new BigDecimal("200.00").setScale(2, RoundingMode.HALF_UP)
        val stock0 = Stock.buildId(ftr1.store, artId0, "", ftr1.company)
        val stock1 = Stock.buildId(ftr1.store, artId1, "", ftr1.company)
        val list2 = list.map(doublePrice)
        for
          oneRow     <- TransactionRepository.create(list2)
          all        <-   TransactionRepository.all(ftr1.modelid, companyId)
          transactionIds:List[(Long, Int)] = all.map(tr =>(tr.id1, tr.modelid))
          postedRows <- TransactionService.postAll(transactionIds, ftr1.company)
          stocks <- StockRepository.getBy(createdStock.map(stock=>stock.id), Stock.MODELID, ftr1.company)
          stock0 <- StockRepository.getById (stock0, Stock.MODELID, ftr1.company)//.debug(s"stock02 >>> ${artId0}")
          stock1 <- StockRepository.getById (stock1, Stock.MODELID, ftr1.company)//.debug(s"stock12 >>> ${artId1}")
          article0 <- ArticleRepository.getById ( artId0, Article.MODELID, ftr1.company)//.debug(s"article02 >>> ${artId0}")
          article1 <- ArticleRepository.getById ( artId1, Article.MODELID, ftr1.company)//.debug(s"article12 >>> ${artId1}")
        yield assertTrue(oneRow ==  list.size+list.flatMap(_.lines).size ) &&
          assertTrue(postedRows == 3) &&
           assertTrue(stocks.size == 2, stock0.quantity.compareTo(quantityStock0)==0
          ,  stock1.quantity.compareTo(quantityStock1)==0
          , article0.avgPrice.compareTo(avgPrice0)==0
          , article0.pprice.compareTo(pPrice0)==0
          , article1.avgPrice.compareTo(avgPrice1)==0
          , article1.pprice.compareTo(pPrice1)==0)
      }

    ).provideLayerShared(testServiceLayer) @@ sequential
}

