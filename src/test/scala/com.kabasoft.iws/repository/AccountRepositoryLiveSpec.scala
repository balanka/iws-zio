package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.Account
import com.kabasoft.iws.domain.AccountBuilder.{companyId, faccountId, fname}
import com.kabasoft.iws.domain.common.zeroAmount
import com.kabasoft.iws.repository.container.PostgresContainer
import zio.ZLayer
import zio.sql.ConnectionPool
import zio.test.TestAspect._
import zio.test._

import java.time.Instant

object AccountRepositoryLiveSpec extends ZIOSpecDefault {

  val accounts = List(
    Account("4713", "MyAccount2", "MyAccount2", Instant.now(), Instant.now(), Instant.now()
      , companyId, 9, "", false, false, "EUR", zeroAmount, zeroAmount, zeroAmount, zeroAmount, Nil.toSet),
    Account("4714", "MyAccount4","MyAccount4", Instant.now(), Instant.now(), Instant.now()
      , companyId, 9, "", false, false, "EUR", zeroAmount, zeroAmount, zeroAmount, zeroAmount, Nil.toSet))


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
          count <- AccountRepository.list(companyId).runCount
        } yield assertTrue(count == 13)
      },
      test("insert two new accounts") {
        for {
          oneRow <- AccountRepository.create(accounts)
          list <- AccountRepository.list(companyId).runCollect.map(_.toList)
          count <- AccountRepository.list(companyId).runCount
        } yield assertTrue(oneRow == 2) && assertTrue(count ==15)&& assertTrue(list.size == 15)
      },
      test("get an account by its id") {
        for {
          stmt <- AccountRepository.getBy(faccountId,companyId)
        } yield assertTrue(stmt.name == fname) && assertTrue(stmt.id == faccountId)
      }
    ).provideLayerShared(testLayer.orDie) @@ sequential
}
