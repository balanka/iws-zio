package com.kabasoft.iws.service

import com.kabasoft.iws.domain.common
import com.kabasoft.iws.domain.AccountBuilder.{company}
import com.kabasoft.iws.domain.TransactionBuilder.{ftr1, line1, line2}
import com.kabasoft.iws.repository.postgresql.PostgresContainer
import com.kabasoft.iws.repository.{JournalRepositoryImpl, PacRepository, PacRepositoryImpl, TransactionRepository, TransactionRepositoryImpl}
import zio.ZLayer
import zio.sql.ConnectionPool
import zio.test.TestAspect._
import zio.test._

import java.time.Instant

object FinancialsServiceLiveSpec extends ZIOSpecDefault {

  val testServiceLayer = ZLayer.make[FinancialsService with TransactionRepository with PacRepository](
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
        val period    =  common.getPeriod(Instant.now())
        for {
          oneRow     <- FinancialsService.create(ftr1)
          ftr        <-   TransactionRepository.all(company).runCollect.map(_.toList)
          postedRows <- FinancialsService.postAll(ftr.map(_.tid), company).map(_.sum)
          oaccountEntry <- FinancialsService.journal(line1.oaccount, period, period, company).map(_.size)
          journalEntries <- FinancialsService.journal(line1.account, period, period, company).map(_.size)
          vatEntry       <- FinancialsService.journal(line2.oaccount, period, period, company).map(_.size)
          nrOfPacs       <-PacRepository.find4Period(line1.account,period, period, company).runCollect.map(_.size)
          balances       <-PacRepository.getBalances4Period(period, period, company).runCollect.map(_.toList)
        } yield {println("balances>>>"+balances);(assertTrue(oneRow == 3)&& assertTrue(postedRows == 10) && assertTrue(nrOfPacs == 1)
        && assertTrue(journalEntries == 2)&& assertTrue(oaccountEntry == 1)&& assertTrue(vatEntry == 1))}
      },
      /*test("getBalances4period for account imported  ") {
        val period    =  common.getPeriod(Instant.now())
        for {
            nrOfPacs       <-PacRepository.getBalances4Period(period, period, company).runCollect.map(_.toList)
        } yield {println("nrOfPacs>>>"+nrOfPacs); assertTrue(nrOfPacs.size ==1)}
      }

       */
    ).provideLayerShared(testServiceLayer.orDie) @@ sequential
}

