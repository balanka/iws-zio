package com.kabasoft.iws.service

import com.kabasoft.iws.domain.common
import com.kabasoft.iws.repository.common.AccountBuilder.{ company, paccountId0}
import com.kabasoft.iws.repository.postgresql.PostgresContainer
import com.kabasoft.iws.repository.{AccountRepositoryImpl, PacRepositoryImpl}
import zio.ZLayer
import zio.sql.ConnectionPool
import zio.test.TestAspect._
import zio.test._

import java.time.{LocalDateTime, ZoneOffset}


object AccountServiceLiveSpec extends ZIOSpecDefault {

  val testServiceLayer = ZLayer.make[AccountService](
    AccountRepositoryImpl.live,
    PacRepositoryImpl.live,
    AccountServiceImpl.live,
    PostgresContainer.connectionPoolConfigLayer,
    ConnectionPool.live,
    PostgresContainer.createContainer
  )

  override def spec =
    suite("Account service  test with postgres test container")(
      test("Get the balance 4 an accounting at the end of a period") {
        val previousYear  =  common.getYear(LocalDateTime.now().minusYears(1).toInstant(ZoneOffset.UTC))
        val fromPeriod    = previousYear.toString.concat("01").toInt
        val toPeriod    =  previousYear.toString.concat("12").toInt
        for {
          accounts       <-AccountService.getBalances(paccountId0, fromPeriod, toPeriod,  company)
        } yield  assertTrue(accounts.size == 1) &&assertTrue(accounts.head.balance == 1000)
      },
      test("Close an accounting  period") {
        val previousYear  =  common.getYear(LocalDateTime.now().minusYears(1).toInstant(ZoneOffset.UTC))
        val fromPeriod    = previousYear.toString.concat("01").toInt
        val toPeriod    =  previousYear.toString.concat("12").toInt
        for {
          nrOfAccounts       <-AccountService.closePeriod(fromPeriod, toPeriod, paccountId0, company)
        } yield  assertTrue(nrOfAccounts == 1)
      }
    ).provideCustomLayerShared(testServiceLayer.orDie) @@ sequential
}

