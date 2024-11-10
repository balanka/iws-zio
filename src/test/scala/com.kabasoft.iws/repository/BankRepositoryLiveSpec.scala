package com.kabasoft.iws.repository

import zio.test._
import zio.test.TestAspect._
import com.kabasoft.iws.domain.{Bank, Masterfile}
import com.kabasoft.iws.repository.container.PostgresContainer

import java.time.Instant
import zio.ZLayer
import zio.sql.ConnectionPool

object BankRepositoryLiveSpec extends ZIOSpecDefault {

 val company ="1000"
  val id = "4712"
  val name = "MyBank"

  val banks = List(
    Masterfile(id, name, "MyBank", "Test", Instant.now(), Instant.now(), Instant.now(), Bank.MODEL_ID, company),
    Masterfile("4713", "MyBank2","MyBank2", "Test", Instant.now(), Instant.now(), Instant.now(), Bank.MODEL_ID, company)
  )

  val testLayer = ZLayer.make[MasterfileRepository](
    MasterfileRepositoryLive.live,
    PostgresContainer.connectionPoolConfigLayer,
    ConnectionPool.live,
    PostgresContainer.createContainer
  )

  override def spec =
    suite("Bank repository test with postgres test container")(
      test("insert two new banks") {
        for {
          oneRow <- MasterfileRepository.create2(banks)
          count <- MasterfileRepository.list((Bank.MODEL_ID, company)).runCount
        } yield assertTrue(oneRow == 2) && assertTrue(count == 4L)
      },
      test("get a Bank by its id") {
        for {
          stmt <- MasterfileRepository.getBy((id, Bank.MODEL_ID, company))
        } yield assertTrue(stmt.name == name) && assertTrue(stmt.id==id)
      }
    ).provideLayerShared(testLayer.orDie) @@ sequential
}
