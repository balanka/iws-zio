package com.kabasoft.iws.service

import com.kabasoft.iws.domain.common
import com.kabasoft.iws.repository.common.AccountBuilder.company
import com.kabasoft.iws.repository.common.TransactionBuilder.{ftr1, pacs}
import com.kabasoft.iws.repository.postgresql.PostgresContainer
import com.kabasoft.iws.repository.{JournalRepositoryImpl, PacRepository, PacRepositoryImpl, TransactionRepositoryImpl}
import zio.ZLayer
import zio.sql.ConnectionPool
import zio.test.TestAspect._
import zio.test._

import java.time.Instant

object FinancialsServiceLiveSpec extends ZIOSpecDefault {

  val testServiceLayer = ZLayer.make[FinancialsService with PacRepository](
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
        for {
          oneRow <- FinancialsService.create(ftr1)
          postedRows <- FinancialsService.post(ftr1, company)
          nrOfPacs       <-PacRepository.getByIds(pacs.map(_.id), company).map(_.size)
        } yield assertTrue(oneRow == 3)&& assertTrue(postedRows == 10)&& assertTrue(nrOfPacs == 1)
      },
      test("getBalances4period for account imported  ") {
        val period    =  common.getPeriod(Instant.now())
        for {
            nrOfPacs       <-PacRepository.getBalances4Period(period, period, company).runCollect.map(_.toList)
        } yield assertTrue(nrOfPacs.size ==1)
      }
    ).provideCustomLayerShared(testServiceLayer.orDie) @@ sequential
}

