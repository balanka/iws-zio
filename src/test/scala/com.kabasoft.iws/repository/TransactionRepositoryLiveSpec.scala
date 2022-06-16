package com.kabasoft.iws.repository

import com.kabasoft.iws.repository.common.AccountBuilder.company
import com.kabasoft.iws.repository.common.TransactionBuilder.{ftr1, transactionId}
import com.kabasoft.iws.repository.postgresql.PostgresContainer
import zio.ZLayer
import zio.sql.ConnectionPool
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test._

object TransactionRepositoryLiveSpec extends ZIOSpecDefault {

  val testLayer = ZLayer.make[TransactionRepository](
    TransactionRepositoryImpl.live,
    PostgresContainer.connectionPoolConfigLayer,
    ConnectionPool.live,
    PostgresContainer.createContainer
  )

  override def spec =
    suite("Transaction repository test with postgres test container")(
      test("insert two new transactions") {
        for {
          oneRow <- TransactionRepository.create(ftr1)
          count <- TransactionRepository.list(company).runCount
        } yield assert(oneRow)(equalTo(3)) && assert(count)(equalTo(2L))
      },
      test("get an transaction by its id") {
        for {
          stmt <- TransactionRepository.getByTransId(transactionId, company)
        } yield  assert(stmt.tid)(equalTo(transactionId))
      }
    ).provideCustomLayerShared(testLayer.orDie) @@ sequential
}
