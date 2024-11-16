package com.kabasoft.iws.repository

import com.kabasoft.iws.config.appConfig
import com.kabasoft.iws.domain.AccountBuilder.companyId
import com.kabasoft.iws.domain.FinancialsTransactionBuilder.pacs
import com.kabasoft.iws.domain.PeriodicAccountBalance
import com.kabasoft.iws.repository.container.PostgresContainer
import com.kabasoft.iws.repository.container.PostgresContainer.appResourcesL
import zio.ZLayer
import zio.test.TestAspect.*
import zio.test.*



object PacRepositoryLiveSpec extends ZIOSpecDefault {


  val testLayer = ZLayer.make[PacRepository](
    PacRepositoryLive.live,
    appResourcesL.project(_.postgres),
    appConfig,
    //PostgresContainer.createContainer
  )


  override def spec =
    suite("Periodic account balance repository  test with postgres test container")(
      test("create 2 pacs and find 1 pac getByIds") {
        for {
          row <- PacRepository.getBy(pacs.map(_.id), PeriodicAccountBalance.MODELID, companyId)
          newPacs=pacs.filterNot(row.contains)
          nrCreatedRow <- PacRepository.create(newPacs)

        } yield assertTrue(nrCreatedRow==1) && assertTrue(row.size==4)
      }
    ).provideLayerShared(testLayer) @@ sequential

}
