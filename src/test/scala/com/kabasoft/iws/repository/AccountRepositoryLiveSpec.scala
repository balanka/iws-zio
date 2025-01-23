package com.kabasoft.iws.repository

import com.kabasoft.iws.config.appConfig
import com.kabasoft.iws.domain.Account
import com.kabasoft.iws.domain.AccountBuilder.{companyId, faccountId}
import com.kabasoft.iws.domain.common.zeroAmount
import com.kabasoft.iws.repository.container.PostgresContainer
import com.kabasoft.iws.repository.container.PostgresContainer.appResourcesL
import zio.ZLayer
import zio.test.TestAspect.*
import zio.test.*

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

  val accIds:List[(String, Int, String)] = accounts.map(acc => (acc.id, acc.modelid, acc.company))

  val testLayer = ZLayer.make[AccountRepository](
    appResourcesL.project(_.postgres),
   // appResourcesL,
    appConfig,
    AccountRepositoryLive.live,
    //PostgresContainer.createContainer
  )

  override def spec =
    suite("Account repository test with postgres test container")(
      test("clear accounts") {
        for {
          deleted <- AccountRepository.deleteAll(accIds)
        } yield assertTrue(deleted == accIds.size)
      },
      test("count all accounts") {
        for {
          count <- AccountRepository.all(Account.MODELID, companyId).map(_.size)
        } yield assertTrue(count == 338)
      },
      test("insert two new accounts") {
        for {
          oneRow <- AccountRepository.create(accounts)
        } yield assertTrue(oneRow == 2) //&& assertTrue(count ==1)&& assertTrue(count == 2)
      },
      test("insert one new account") {
        for {
          //oneRow <- AccountRepository.create(newAccount)
          acc <- AccountRepository.getById((newAccount.id, Account.MODELID, newAccount.company))
        } yield  assertTrue(acc.id == newAccount.id)
      },
      test("get an account by its id") {
        for {
          stmt <- AccountRepository.getBy( accounts.map(_.id), Account.MODELID, companyId)
        } yield assertTrue(stmt.size ==2) //&& assertTrue(stmt.map(_.id).contains(faccountId)) //&&
                //assertTrue(stmt.map(_.id).contains(newAccount.id))
      }
    ).provideLayerShared(testLayer) @@ sequential
}
