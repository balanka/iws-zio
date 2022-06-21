package com.kabasoft.iws.service

import com.kabasoft.iws.repository.common.AccountBuilder.company
import com.kabasoft.iws.repository.common.TransactionBuilder.{dtransactions, ftr1, pacs}
import com.kabasoft.iws.repository.postgresql.PostgresContainer
import com.kabasoft.iws.repository.{JournalRepositoryImpl, PacRepositoryImpl, TransactionRepositoryImpl}
import zio.ZLayer
import zio.sql.ConnectionPool
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test._

object FinancialsServiceLiveSpec extends ZIOSpecDefault {

  val testServiceLayer = ZLayer.make[FinancialsService /*with PacRepository*/](
    PacRepositoryImpl.live,
    JournalRepositoryImpl.live,
    TransactionRepositoryImpl.live,
    FinancialsServiceImpl.live,
    PostgresContainer.connectionPoolConfigLayer,
    ConnectionPool.live,
    PostgresContainer.createContainer
  )

  override def spec =
    suite("Financials service  test with postgres test container")(
      test("create and post 2 transactions") {
        for {
          oneRow <- FinancialsService.create(ftr1)
          postedRows <- FinancialsService.postAll(dtransactions.map(_.id).distinct, company).map(_.sum)
          nrOfPacs       <-FinancialsService.getByIds(pacs.map(_.id), company).map(_.size)
        } yield assert(oneRow)(equalTo(3))&& assert(postedRows)(equalTo(10))&& assert(nrOfPacs)(equalTo(1))
      },
      /*test("get an transaction by its id") {
        for {
          stmt <- TransactionRepository.getBy(lineTransactionId1.toString,company)
        } yield  assert(stmt.id)(equalTo(lineTransactionId1))
      }

       */
    ).provideCustomLayerShared(testServiceLayer.orDie) @@ sequential
}

