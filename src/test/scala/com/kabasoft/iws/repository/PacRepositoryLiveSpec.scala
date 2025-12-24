package com.kabasoft.iws.repository

import com.kabasoft.iws.config.appConfig
import com.kabasoft.iws.domain.AccountBuilder.{companyId, faccountId, incaccountId1}
import com.kabasoft.iws.domain.FinancialsTransactionBuilder.pacs
import com.kabasoft.iws.domain.PeriodicAccountBalance
import com.kabasoft.iws.repository.container.PostgresContainer
import com.kabasoft.iws.repository.container.PostgresContainer.appResourcesL
import com.kabasoft.iws.repository.container.PostgresContainer
import com.kabasoft.iws.repository.container.PostgresContainer.appResourcesL
import zio.ZLayer
import zio.test.TestAspect.*
import zio.test.*
import java.time.Instant
import java.math.RoundingMode

object PacRepositoryLiveSpec extends ZIOSpecDefault {
  
  val testLayer = ZLayer.make[PacRepository](
    PacRepositoryLive.live,
    appResourcesL.project(_.postgres),
    appConfig,
    //PostgresContainer.createContainer
  )


  override def spec =
    suite("Periodic account balance repository  test with postgres test container")(
      test("clear PACs ") {
        for
          deleted <- PacRepository.deleteAll()
        yield assertTrue(deleted ==1) 
      },
      test("create 3 pacs") {
        for {
          nrCreatedRow <- PacRepository.create(pacs)
        } yield assertTrue(nrCreatedRow==3) 
      },
      test(" search and find pacs by  getById, getBy, all, findBalance4Period, find4AccountPeriod") {
        val now = Instant.now
        val dummy= PeriodicAccountBalance.dummy
        val toPeriod = com.kabasoft.iws.domain.common.getPeriod(now)
        val fromPeriod = (toPeriod.toString.substring(0, 3)+"00").toInt
        val oneId = pacs.map(_.id).headOption.getOrElse("-1")
        val amount = new java.math.BigDecimal("100.00").setScale(2, RoundingMode.HALF_UP)
        for {
          row <- PacRepository.getBy(pacs.map(_.id), PeriodicAccountBalance.MODELID, companyId)
          all <- PacRepository.all(PeriodicAccountBalance.MODELID, companyId)
          one <- PacRepository.getById(oneId, PeriodicAccountBalance.MODELID, companyId)
          balance4Period <- PacRepository.findBalance4Period( com.kabasoft.iws.domain.common.getPeriod(Instant.now), companyId)
          balance4AccountPeriod <- PacRepository.find4AccountPeriod(faccountId, fromPeriod, toPeriod, companyId)
        } yield  assertTrue(row.size==3) &&
          assertTrue(all.size==3) && assertTrue(one.id==oneId) &&
          assertTrue(balance4Period.size ==3) && assertTrue(balance4AccountPeriod.size==1
          , balance4AccountPeriod.headOption.getOrElse(dummy).debit.equals(amount)) 
      },
      test(" search and find a pac by find4AccountPeriod and update it") {
        val now = Instant.now
        val dummy= PeriodicAccountBalance.dummy
        val toPeriod = com.kabasoft.iws.domain.common.getPeriod(now)
        val fromPeriod = (toPeriod.toString.substring(0, 3)+"00").toInt
        for {
          pac2Update <- PacRepository.find4AccountPeriod(incaccountId1, fromPeriod, toPeriod, companyId).map(_.headOption.getOrElse(dummy))
          udatedPac = pac2Update.copy(debit = pac2Update.credit)
          updated <- PacRepository.update(List(udatedPac))
        } yield assertTrue(pac2Update.account.equals(incaccountId1))  && assertTrue(updated == 1)
      }
    ).provideLayerShared(testLayer) @@ sequential

}
