package com.kabasoft.iws.repository

import zio.test._
import zio.test.TestAspect._
import com.kabasoft.iws.repository.common.BankStatementBuilder.{bs, company, id}
import zio.ZLayer
import com.kabasoft.iws.repository.postgresql.PostgresContainer
import zio.sql.ConnectionPool

object BankStatementRepositoryLiveSpec extends ZIOSpecDefault {

  val testLayer = ZLayer.make[BankStatementRepository](
    BankStatementRepositoryImpl.live,
    PostgresContainer.connectionPoolConfigLayer,
    ConnectionPool.live,
    PostgresContainer.createContainer
  )

  override def spec =
    suite("Bankstatement repository test with postgres test container")(
      test("count all bankstatements") {
        for {
          count <- BankStatementRepository.list(company).runCount
        } yield assertTrue(count == 1L)
      },
      test("insert two new bankstatements") {
        for {
          oneRow <- BankStatementRepository.create(bs)
          count <- BankStatementRepository.list(company).runCount
        } yield assertTrue(oneRow == 2) && assertTrue(count == 3L)
      },
      test("get a BankStatement by its id") {
        for {
          stmt <- BankStatementRepository.getBy(id,company)
        } yield assertTrue(stmt.depositor == "B Mady") && assertTrue(stmt.id == id.toLong)
      }
    ).provideCustomLayerShared(testLayer.orDie) @@ sequential
}
