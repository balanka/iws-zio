package com.kabasoft.iws.repository

import com.kabasoft.iws.repository.common.AccountBuilder.company
import com.kabasoft.iws.repository.common.TransactionBuilder.pacs
import com.kabasoft.iws.repository.postgresql.PostgresContainer
import zio.ZLayer
import zio.sql.ConnectionPool
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test._



object PacRepositoryLiveSpec extends ZIOSpecDefault {


  val testLayer = ZLayer.make[PacRepository](
    PacRepositoryImpl.live,
    PostgresContainer.connectionPoolConfigLayer,
    ConnectionPool.live,
    PostgresContainer.createContainer
  )


  override def spec =
    suite("Periodic account balance repository  test with postgres test container")(
      test("create 2 pacs and find 1 pac getByIds") {
        for {
          row <- PacRepository.getByIds(pacs.map(_.id), company)
          newPacs=pacs.filterNot(row.contains)
          nrCreatedRow <- PacRepository.create(newPacs)

        } yield assert(nrCreatedRow)(equalTo(2)) && assert(row.size)(equalTo(1))
      }
    ).provideCustomLayerShared(testLayer.orDie) @@ sequential

}
