package com.kabasoft.iws.repository

import com.kabasoft.iws.config.appConfig
import zio.test.*
import zio.test.TestAspect.*
import com.kabasoft.iws.domain.{Bank, Masterfile}
import com.kabasoft.iws.repository.container.PostgresContainer
import com.kabasoft.iws.repository.container.PostgresContainer.appResourcesL
import java.time.Instant
import zio.ZLayer

object BankRepositoryLiveSpec extends ZIOSpecDefault {

 val company ="-1000"
  val id = "4712"
  val name = "MyBank"

  val banks = List(
    Masterfile(id, name, "MyBank", "Test", Instant.now(), Instant.now(), Instant.now(), Bank.MODEL_ID, company),
    Masterfile("4713", "MyBank2","MyBank2", "Test", Instant.now(), Instant.now(), Instant.now(), Bank.MODEL_ID, company)
  )

  val testLayer = ZLayer.make[MasterfileRepository](
    appResourcesL.project(_.postgres),
    appConfig,
    MasterfileRepositoryLive.live,
    //PostgresContainer.createContainer
  )
  val banksIds: List[(String, Int, String)] = banks.map(acc => (acc.id, acc.modelid, acc.company))
  override def spec =
    suite("Bank repository test with postgres test container")(
      test("clear banks") {
          for {
            deleted <- MasterfileRepository.deleteAll(banksIds)
          } yield assertTrue(deleted == banksIds.size)
        },
      test("insert two new banks") {
        for {
          oneRow <- MasterfileRepository.create(banks)
          count <- MasterfileRepository.all((Bank.MODEL_ID, company)).map(_.size)
        } yield assertTrue(oneRow == 2) && assertTrue(count == 2)
      },
      test("get a Bank by its id") {
        for {
          stmt <- MasterfileRepository.getById((id, Bank.MODEL_ID, company))
        } yield assertTrue(stmt.name == name) && assertTrue(stmt.id==id)
      }
    ).provideLayerShared(testLayer) @@ sequential
}
