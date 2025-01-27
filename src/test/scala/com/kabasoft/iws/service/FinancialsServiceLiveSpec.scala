package com.kabasoft.iws.service

import com.kabasoft.iws.config.appConfig
import com.kabasoft.iws.domain.{Balance, PeriodicAccountBalance, common}
import com.kabasoft.iws.domain.common.given 
import com.kabasoft.iws.domain.AccountBuilder.{companyId, paccountId0, zero}
import com.kabasoft.iws.domain.FinancialsTransactionBuilder.{ftr1, ftr2, line1, line2}
import com.kabasoft.iws.repository.container.PostgresContainer
import com.kabasoft.iws.repository.container.PostgresContainer.appResourcesL
import com.kabasoft.iws.repository.{AccountRepository, AccountRepositoryLive, FinancialsTransactionRepository
  , FinancialsTransactionRepositoryLive, JournalRepository, JournalRepositoryLive, PacRepository, PacRepositoryLive
  , PostFinancialsTransactionRepositoryLive, PostTransactionRepositoryLive}
import zio.ZLayer
import zio.test.TestAspect.*
import zio.test.*
import java.math.{BigDecimal, RoundingMode}
import java.time.{Instant, LocalDateTime, ZoneOffset}


object FinancialsServiceLiveSpec extends ZIOSpecDefault {
  
  val testServiceLayer = ZLayer.make[AccountService & FinancialsService & FinancialsTransactionRepository & 
    AccountRepository & PacRepository & JournalRepository](
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
  val list = List(ftr1, ftr2)
  val ids = list.map(_.id)
  val pacIds  = list.flatMap(FinancialsService.buildPacIds)
  override def spec =
    suite("Financials service  test with postgres test container")(
      test("clear financials transactions ") {
        for
          deletedTransactions <- FinancialsTransactionRepository.deleteAll(list)
          deletedPacs <- PacRepository.deleteAll()
          deletedJournal <- JournalRepository.deleteAllTest()
        yield assertTrue(deletedTransactions == 1) &&
              assertTrue(deletedPacs ==1) &&
              assertTrue(deletedJournal == 1)
      },
      test("create  2 transaction") {
        for 
          oneRow     <- FinancialsTransactionRepository.create(list)
         yield assertTrue(oneRow+list.size == 7 ) 
      },
      test("create and post 1 transaction") {

        //val previousYear = common.getYear(LocalDateTime.now().minusYears(1).toInstant(ZoneOffset.UTC))
        val period    =  common.getPeriod(Instant.now())
        val currentYear = common.getYear(Instant.now())
        val toPeriod = currentYear.toString.concat("01").toInt
        //val toPPeriod = previousYear.toString.concat("12").toInt
        val amount = new BigDecimal("200.00").setScale(2, RoundingMode.HALF_UP)
        //val amount2 = new BigDecimal("769.00").setScale(2, RoundingMode.HALF_UP)
        //val creditAmount = new BigDecimal("200.00").setScale(2, RoundingMode.HALF_UP)

        for {
          ftr        <-   FinancialsTransactionRepository.all(ftr1.modelid, ftr1.company)
          postedRows <- FinancialsService.postAll(ftr.map(_.id), companyId)
          oaccountEntry <- FinancialsService.journal(line1.oaccount, period, period, companyId).map(_.size)
          accountEntry <- FinancialsService.journal(line1.account, period, period, companyId).map(_.size)
          vatEntry       <- FinancialsService.journal(line2.oaccount, period, period, companyId).map(_.size)
          nrOfAccounts       <-AccountService.closePeriod(toPeriod, paccountId0, companyId)
          nrOfPacs       <-PacRepository.find4AccountPeriod(line1.account, period, companyId).map(_.size)
          balances4P     <-PacRepository.findBalance4Period(period, companyId)
          // balance       <-AccountService.getBalance(paccountId0, toPeriod, companyId).map(_.head)
          bal = balances4P.map(pac=>(pac.period.toString.substring(0,4), pac.idebit, pac.icredit, pac.debit, pac.credit))
            .groupBy(_._1).map { case (_, v) => common.reduce(v.map(Balance.apply), common.dummyBalance)}
            .toList.headOption.getOrElse(common.dummyBalance)
          account      <-AccountService.getBalance(paccountId0, toPeriod,  companyId).head
        } yield {
          assertTrue( nrOfAccounts == 2, postedRows == 13, nrOfPacs == 1, accountEntry == 2,
            oaccountEntry == 1, vatEntry == 1, balances4P.size == 3 ) &&
            assertTrue(bal.debit.equals(bal.credit)) &&
            assertTrue(account.id == paccountId0) &&assertTrue(account.debit.equals(account.credit))
          //assertTrue(balance.debit.equals(balance.credit))
        }
      }
    ).provideLayerShared(testServiceLayer) @@ sequential
}

