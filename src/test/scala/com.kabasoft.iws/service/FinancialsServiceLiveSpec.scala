package com.kabasoft.iws.service

import com.kabasoft.iws.domain.{PeriodicAccountBalance, common}
import com.kabasoft.iws.domain.AccountBuilder.{company, paccountId0}
import com.kabasoft.iws.domain.TransactionBuilder.{ftr1, ftr2, line1, line2,line3}
import com.kabasoft.iws.repository.postgresql.PostgresContainer
import com.kabasoft.iws.repository.{AccountRepository, AccountRepositoryImpl, JournalRepositoryImpl, PacRepository, PacRepositoryImpl, TransactionRepository, TransactionRepositoryImpl}
import zio.ZLayer
import zio.sql.ConnectionPool
import zio.test.TestAspect._
import zio.test._

import java.time.{Instant, LocalDateTime, ZoneOffset}

object FinancialsServiceLiveSpec extends ZIOSpecDefault {

  val testServiceLayer = ZLayer.make[AccountService  with FinancialsService with TransactionRepository with AccountRepository with PacRepository](
    AccountRepositoryImpl.live,
    AccountServiceImpl.live,
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
      test("create and post 1 transaction") {

        val previousYear = common.getYear(LocalDateTime.now().minusYears(1).toInstant(ZoneOffset.UTC))
        val period    =  common.getPeriod(Instant.now())
        val currentYear = common.getYear(Instant.now())
        val fromPeriod = currentYear.toString.concat("01").toInt
        val toPeriod = currentYear.toString.concat("12").toInt
        val fromPPeriod = previousYear.toString.concat("01").toInt
        val toPPeriod = previousYear.toString.concat("12").toInt
        for {
          oneRow     <- TransactionRepository.create2(List(ftr1, ftr2))
          ftr        <-   TransactionRepository.all(company).runCollect.map(_.toList)
          postedRows <- FinancialsService.postAll(ftr.map(_.tid), company).map(_.sum)
          oaccountEntry <- FinancialsService.journal(line1.oaccount, period, period, company).map(_.size)
          accountEntry <- FinancialsService.journal(line1.account, period, period, company).map(_.size)
          vatEntry       <- FinancialsService.journal(line2.oaccount, period, period, company).map(_.size)
          nrOfAccounts       <-AccountService.closePeriod(fromPPeriod, toPPeriod, paccountId0, company)
          nrOfPacs       <-PacRepository.find4Period(line1.account,period, period, company).runCollect.map(_.size)
          balances4P     <-PacRepository.getBalances4Period(period, period, company).runCollect.map(_.toList)

          balance       <-AccountService.getBalance(paccountId0, fromPeriod, toPeriod, company).map(_.toList.head)
        } yield {;
          (assertTrue(oneRow == 5)&&
          assertTrue(postedRows == 17) &&
            assertTrue(nrOfPacs == 1) && assertTrue(accountEntry == 3) &&
          assertTrue(oaccountEntry == 1)&& assertTrue(vatEntry == 1) && assertTrue(balances4P.size == 2) &&
          assertTrue(balances4P.headOption.getOrElse(PeriodicAccountBalance.dummy).debit == 119) &&
          assertTrue(balance.debit == 238) && assertTrue(balance.credit == 119))
        }
      }
    ).provideLayerShared(testServiceLayer.orDie) @@ sequential
}

