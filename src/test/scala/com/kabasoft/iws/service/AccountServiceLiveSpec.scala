package com.kabasoft.iws.service

import com.kabasoft.iws.config.appConfig
import com.kabasoft.iws.domain.common
import com.kabasoft.iws.domain.AccountBuilder.{companyId, paccountId0, raccountId}
import com.kabasoft.iws.repository.container.PostgresContainer
import com.kabasoft.iws.repository.container.PostgresContainer.appResourcesL
import com.kabasoft.iws.repository.{AccountRepositoryLive, PacRepositoryLive}
import zio.ZLayer
import zio.test.TestAspect.*
import zio.test.*

import java.math.{BigDecimal, RoundingMode}
import java.time.{LocalDateTime, ZoneOffset}


object AccountServiceLiveSpec extends ZIOSpecDefault {

  val testServiceLayer = ZLayer.make[AccountService](
    appResourcesL.project(_.postgres),
    appConfig,
    AccountRepositoryLive.live,
    PacRepositoryLive.live,
    AccountServiceLive.live,
    //PostgresContainer.createContainer
  )

  override def spec =
    suite("Account service  test with postgres test container")(
      test("Get the balance 4 an accounting at the end of a period") {
        val currentYear  =  common.getYear(LocalDateTime.now().toInstant(ZoneOffset.UTC))
        val toPeriod    =  currentYear.toString.concat("12").toInt
        val amount = new BigDecimal("1050.00").setScale(2, RoundingMode.UNNECESSARY)
        for {
          account      <-AccountService.getBalance(paccountId0, toPeriod,  companyId).head
        } yield  assertTrue(account.id == paccountId0) &&assertTrue(account.balance.equals(amount) )
      },
      test("Close an accounting  period") {
        val previousYear  =  common.getYear(LocalDateTime.now().minusYears(1).toInstant(ZoneOffset.UTC))
        val toPeriod    =  previousYear.toString.concat("12").toInt
        for {
          nrOfAccounts       <-AccountService.closePeriod(toPeriod, raccountId, companyId)
        } yield  assertTrue(nrOfAccounts == 2)
      }
    ).provideLayerShared(testServiceLayer) @@ sequential
}

