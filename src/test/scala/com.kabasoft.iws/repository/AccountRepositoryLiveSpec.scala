package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.Account
import com.kabasoft.iws.domain.AccountBuilder.{company, faccountId, fname}
import com.kabasoft.iws.repository.postgresql.PostgresContainer
import zio.ZLayer
import zio.sql.ConnectionPool
import zio.test.TestAspect._
import zio.test._

import java.time.Instant

object AccountRepositoryLiveSpec extends ZIOSpecDefault {



  val accounts = List(
    Account("4713", "MyAccount2", "MyAccount2", Instant.now(), Instant.now(), Instant.now()
      , company, 9, "", false, false, "EUR", BigDecimal(0), BigDecimal(0), BigDecimal(0), BigDecimal(0), Nil.toSet),
    Account("4714", "MyAccount4","MyAccount4", Instant.now(), Instant.now(), Instant.now()
      , company, 9, "", false, false, "EUR", BigDecimal(0), BigDecimal(0), BigDecimal(0), BigDecimal(0), Nil.toSet))


  val testLayer = ZLayer.make[AccountRepository](
    AccountRepositoryImpl.live,
    PostgresContainer.connectionPoolConfigLayer,
    ConnectionPool.live,
    PostgresContainer.createContainer
  )

  override def spec =
    suite("Account repository test with postgres test container")(
      test("count all accounts") {
        for {
          count <- AccountRepository.list(company).runCount
        } yield assertTrue(count == 11)
      },
      test("insert two new accounts") {
        for {
          oneRow <- AccountRepository.create(accounts)
          list <- AccountRepository.list(company).runCollect.map(_.toList)
          count <- AccountRepository.list(company).runCount
        } yield assertTrue(oneRow == 2) && assertTrue(count ==13)&& assertTrue(list.size == 13)
      },
      test("get an account by its id") {
        for {
          stmt <- AccountRepository.getBy(faccountId,company)
        } yield assertTrue(stmt.name == fname) && assertTrue(stmt.id == faccountId)
      }
    ).provideLayerShared(testLayer.orDie) @@ sequential
}
