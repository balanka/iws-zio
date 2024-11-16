package com.kabasoft.iws.service

import com.kabasoft.iws.config.appConfig
import com.kabasoft.iws.domain.{PeriodicAccountBalance, common}
import com.kabasoft.iws.domain.AccountBuilder.{companyId, paccountId0}
import com.kabasoft.iws.domain.FinancialsTransactionBuilder.{ftr1, ftr2, line1, line2}
import com.kabasoft.iws.repository.container.PostgresContainer
import com.kabasoft.iws.repository.container.PostgresContainer.appResourcesL
import com.kabasoft.iws.repository.{AccountRepository, AccountRepositoryLive, FinancialsTransactionRepository, FinancialsTransactionRepositoryLive, JournalRepositoryLive, PacRepository, PacRepositoryLive, PostFinancialsTransactionRepositoryLive, PostTransactionRepositoryLive}
import zio.ZLayer
import zio.test.TestAspect.*
import zio.test.*
import java.math.{BigDecimal, RoundingMode}
import java.time.{Instant, LocalDateTime, ZoneOffset}


object FinancialsServiceLiveSpec extends ZIOSpecDefault {

  val testServiceLayer = ZLayer.make[AccountService & FinancialsService & FinancialsTransactionRepository & AccountRepository & PacRepository](
    appResourcesL.project(_.postgres),
    appConfig,
    AccountRepositoryLive.live,
    AccountServiceLive.live,
    PacRepositoryLive.live,
    JournalRepositoryLive.live,
    FinancialsTransactionRepositoryLive.live,
    FinancialsServiceLive.live,
    PostFinancialsTransactionRepositoryLive.live,
    //PostgresContainer.createContainer
  )

  override def spec =
    suite("Financials service  test with postgres test container")(
      test("create and post 1 transaction") {

        val previousYear = common.getYear(LocalDateTime.now().minusYears(1).toInstant(ZoneOffset.UTC))
        val period    =  common.getPeriod(Instant.now())
        val currentYear = common.getYear(Instant.now())
        val toPeriod = currentYear.toString.concat("12").toInt
        val toPPeriod = previousYear.toString.concat("12").toInt
        val amount = new BigDecimal("200.00").setScale(2, RoundingMode.HALF_UP)
        val amount2 = new BigDecimal("769.00").setScale(2, RoundingMode.HALF_UP)
        val creditAmount = new BigDecimal("200.00").setScale(2, RoundingMode.HALF_UP)

        val list  = List(ftr1, ftr2)
        for {

          oneRow     <- FinancialsTransactionRepository.create(list)
          ftr        <-   FinancialsTransactionRepository.all(ftr1.modelid, ftr1.company)
          postedRows <- FinancialsService.postAll(ftr.map(_.id), companyId)
          oaccountEntry <- FinancialsService.journal(line1.oaccount, period, period, companyId).map(_.size)
          accountEntry <- FinancialsService.journal(line1.account, period, period, companyId).map(_.size)
          vatEntry       <- FinancialsService.journal(line2.oaccount, period, period, companyId).map(_.size)
          nrOfAccounts       <-AccountService.closePeriod(toPPeriod, paccountId0, companyId)
          nrOfPacs       <-PacRepository.find4AccountPeriod(line1.account, period, companyId).map(_.size)
          balances4P     <-PacRepository.findBalance4Period(period, companyId)
          balance       <-AccountService.getBalance(paccountId0, toPeriod, companyId).map(_.head)
        } yield {
          assertTrue(oneRow+list.size == 5, nrOfAccounts == 1, postedRows == 21, nrOfPacs == 1, accountEntry == 3,
            oaccountEntry == 1, vatEntry == 1, balances4P.size == 5,
            balances4P.headOption.getOrElse(PeriodicAccountBalance.dummy).debit.equals(amount),
            balance.debit.equals(amount2), balance.credit.equals(creditAmount))
        }
      }
    ).provideLayerShared(testServiceLayer) @@ sequential
}

