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
    suite("Financials service  test with postgres test container")(
      test("create and post 2 transactions") {
        for {
          oneRow <- PacRepository.getByIds(pacs.map(_.id), company).map(_.toList.size)
        } yield assert(oneRow)(equalTo(3))
      }
    ).provideCustomLayerShared(testLayer.orDie) @@ sequential

}
