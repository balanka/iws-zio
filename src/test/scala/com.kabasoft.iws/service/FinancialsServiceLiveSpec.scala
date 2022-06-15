package com.kabasoft.iws.service

import com.kabasoft.iws.repository.common.AccountBuilder.company
import com.kabasoft.iws.repository.common.TransactionBuilder.ftr1
import com.kabasoft.iws.repository.postgresql.PostgresContainer
import com.kabasoft.iws.repository.{JournalRepositoryImpl, PacRepositoryImpl,  TransactionRepositoryImpl}

import zio.ZLayer
import zio.sql.ConnectionPool
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test._

object FinancialsServiceLiveSpec extends ZIOSpecDefault {

  val testServiceLayer = ZLayer.make[FinancialsService](
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
          oneRow <- FinancialsService.create(ftr1.toDerive())
          postedRows <- FinancialsService.postAll(ftr1.toDerive().map(_.id), company).map(_.sum)
        } yield assert(oneRow)(equalTo(2))&& assert(postedRows)(equalTo(8))
      }/*,
      test("get an transaction by its id") {
        for {
          stmt <- TransactionRepository.getBy(lineTransactionId1.toString,company)
        } yield  assert(stmt.id)(equalTo(lineTransactionId1))
      }*/
    ).provideCustomLayerShared(testServiceLayer.orDie) @@ sequential
}

