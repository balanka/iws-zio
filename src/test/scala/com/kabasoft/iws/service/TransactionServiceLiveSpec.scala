package com.kabasoft.iws.service

import com.kabasoft.iws.config.appConfig
import com.kabasoft.iws.domain.AccountBuilder.companyId
import com.kabasoft.iws.domain.ArticleBuilder.{artId0, artId1}
import com.kabasoft.iws.domain.TransactionBuilder.{ftr1, ftr2, ftr3, ftr4, ftr5, ftr6}
import com.kabasoft.iws.domain.{Article, Stock}
import com.kabasoft.iws.repository.container.PostgresContainer
import com.kabasoft.iws.repository.*
import com.kabasoft.iws.repository.container.PostgresContainer.appResourcesL
import zio.ZLayer
import zio.prelude.FlipOps
import zio.test.TestAspect.*
import zio.test.*

import java.math.{BigDecimal, RoundingMode}

object TransactionServiceLiveSpec extends ZIOSpecDefault {

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

  override def spec =
    suite("TransactionService  test with postgres test container")(
      test("create and post 1 transaction") {
        val list  = List(ftr1, ftr2, ftr3, ftr4, ftr5, ftr6)
        val createdStock = Stock.create(list)
        val ids = createdStock.map(_.id)
        val avgPrice0 =  new BigDecimal("10.00").setScale(2, RoundingMode.HALF_UP)
        val avgPrice1 =  new BigDecimal("25.00").setScale(2, RoundingMode.HALF_UP)
        val pPrice0 =  new BigDecimal("10.00").setScale(2, RoundingMode.HALF_UP)
        val pPrice1 =  new BigDecimal("50.00").setScale(2, RoundingMode.HALF_UP)
        val zero =  new BigDecimal("0.00").setScale(2, RoundingMode.HALF_UP)
        for {
          oneRow     <- TransactionRepository.create(list)
          all        <-   TransactionRepository.all(ftr1.modelid, companyId)
          transactionIds:List[(Long, Int)] = all.map(tr =>(tr.id, tr.modelid))
          postedRows <- TransactionService.postAll(transactionIds, ftr1.company)
          stocks <- StockRepository.getBy(createdStock.map(stock=>stock.id), Stock.MODELID, ftr1.company)
          stock0 <- StockRepository.getById (Stock.buildId(ftr1.store, artId0, "", ftr1.company), Stock.MODELID, ftr1.company).debug(s"stock0 >>> ${artId0}")
          stock1 <- StockRepository.getById ( Stock.buildId(ftr1.store, artId1, "", ftr1.company), Stock.MODELID, ftr1.company).debug(s"stock1 >>> ${artId1}")
          article0 <- ArticleRepository.getById ( artId0, Article.MODELID, ftr1.company).debug(s"article0 >>> ${artId0}")
          article1 <- ArticleRepository.getById ( artId1, Article.MODELID, ftr1.company).debug(s"article1 >>> ${artId1}")

        } yield {
          assertTrue(oneRow == 18,  postedRows == 31, stocks.size == 2, stock0.quantity.compareTo(zero)==0
          ,  stock1.quantity.compareTo(zero)==0, article0.avgPrice.compareTo(avgPrice0)==0
            , article0.pprice.compareTo(pPrice0)==0
            , article1.avgPrice.compareTo(avgPrice1)==0,  article1.pprice.compareTo(pPrice1)==0)
        }
      }

    ).provideLayerShared(testServiceLayer) @@ sequential
}

