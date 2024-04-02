package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AccountBuilder.companyId
import com.kabasoft.iws.domain.ArticleBuilder.{artId0, artId1}
import com.kabasoft.iws.domain.TransactionBuilder.{ftr1, ftr2, ftr3}
import com.kabasoft.iws.domain.common.zeroAmount
import com.kabasoft.iws.domain.Stock
import com.kabasoft.iws.repository.container.PostgresContainer
import com.kabasoft.iws.repository._
import zio.ZLayer
import zio.prelude.FlipOps
import zio.sql.ConnectionPool
import zio.test.TestAspect._
import zio.test._

import java.math.{BigDecimal, RoundingMode}

object TransactionServiceLiveSpec extends ZIOSpecDefault {

  val testServiceLayer = ZLayer.make[AccountService  with TransactionService with TransactionRepository with PostOrder
    with TransactionLogRepository with ArticleRepository with AccountRepository with PacRepository with StockRepository
    with PostGoodreceiving with PostBillOfDelivery](
    AccountRepositoryImpl.live,
    AccountServiceImpl.live,
    ArticleRepositoryImpl.live,
    StockRepositoryImpl.live,
    PacRepositoryImpl.live,
    JournalRepositoryImpl.live,
    TransactionRepositoryImpl.live,
    TransactionLogRepositoryImpl.live,
    TransactionServiceImpl.live,
    PostTransactionRepositoryImpl.live,
    PostOrderImpl.live,
    PostGoodreceivingImpl.live,
    PostBillOfDeliveryImpl.live,
    PostgresContainer.connectionPoolConfigLayer,
    ConnectionPool.live,
    PostgresContainer.createContainer
  )

  override def spec =
    suite("TransactionService  test with postgres test container")(
      test("create and post 1 transaction") {
        val list  = List(ftr1, ftr2, ftr3)
        val ids = Stock.create(list).map(_.id)
        val avgPrice0 =  new BigDecimal("10.00").setScale(2, RoundingMode.HALF_UP)
        val avgPrice1 =  new BigDecimal("25.00").setScale(2, RoundingMode.HALF_UP)
        val zero =  new BigDecimal("0.00").setScale(2, RoundingMode.HALF_UP)

        val amount =  new BigDecimal("100.00").setScale(2, RoundingMode.HALF_UP)
        for {
          oneRow     <- TransactionRepository.create2(list)
          ftr        <-   TransactionRepository.all(companyId)
          postedRows <- TransactionService.postAll(ftr.map(_.id), companyId)
         // postedRows <- TransactionService.postAll(List(ftr1.modelid), companyId)
          stocks <- ids.map(StockRepository.getById).flip
          stock0 <- StockRepository.getBy (( ftr1.store, artId0), ftr1.company).debug(s"stock0 >>> ${artId0}")
          stock1 <- StockRepository.getBy (( ftr1.store, artId1), ftr1.company).debug(s"stock1 >>> ${artId1}")
          article0 <- ArticleRepository.getBy ( (artId0, ftr1.company)).debug(s"article0 >>> ${artId0}")
          article1 <- ArticleRepository.getBy ( (artId1, ftr1.company)).debug(s"article1 >>> ${artId1}")

        } yield {
          assertTrue(oneRow == 9,  postedRows == 24, stocks.size == 2, stock0.getOrElse(Stock.dummy).quantity.compareTo(zero)==0
          ,  stock1.getOrElse(Stock.dummy).quantity.compareTo(zero)==0, article0.avgPrice.compareTo(avgPrice0)==0
             , article1.avgPrice.compareTo(avgPrice1)==0)
        }
      },

    ).provideLayerShared(testServiceLayer.orDie) @@ sequential
}

