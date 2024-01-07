package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.Account
import com.kabasoft.iws.domain.AccountBuilder.{companyId, faccountId}
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
      , companyId, 9, "", false, false, "EUR", zeroAmount, zeroAmount, zeroAmount, zeroAmount, Nil.toSet)
   )
  val newAccount = Account("000001", "Dummy", "Dummy", Instant.now(), Instant.now(), Instant.now(), companyId, Account.MODELID,
    "5", true, true, "EUR", zeroAmount, zeroAmount, zeroAmount, zeroAmount, Nil.toSet)



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
          count <- AccountRepository.list(Account.MODELID, companyId).runCount
        } yield assertTrue(count == 17)
      },
      test("insert two new accounts") {
        for {
          oneRow <- AccountRepository.create2(accounts)
          list <- AccountRepository.list(Account.MODELID, companyId).runCollect.map(_.toList)
          count <- AccountRepository.list(Account.MODELID, companyId).runCount
        } yield assertTrue(oneRow == 2) && assertTrue(count ==19)&& assertTrue(list.size == 19)
      },
      test("insert one new account") {
        for {
          oneRow <- AccountRepository.create(newAccount)
          acc <- AccountRepository.getBy((newAccount.id, newAccount.company))
        } yield assertTrue(oneRow.id == newAccount.id) && assertTrue(acc.id == newAccount.id)
      },
      test("get an account by its id") {
        for {
          stmt <- AccountRepository.getBy(List(faccountId, newAccount.id),companyId)
        } yield assertTrue(stmt.size ==2) && assertTrue(stmt.map(_.id).contains(faccountId)) &&
                assertTrue(stmt.map(_.id).contains(newAccount.id))
      }
    ).provideLayerShared(testLayer.orDie) @@ sequential
}
