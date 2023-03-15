package com.kabasoft.iws.repository

import zio.test._
import zio.test.TestAspect._
import com.kabasoft.iws.domain.Bank
import com.kabasoft.iws.repository.container.PostgresContainer

import java.time.Instant
import zio.ZLayer
import zio.sql.ConnectionPool

object BankRepositoryLiveSpec extends ZIOSpecDefault {

 val company ="1000"
  val id = "4712"
  val name = "MyBank"

  val banks = List(
    Bank(id, name,"MyBank", Instant.now(), Instant.now(), Instant.now(),11,"1000"),
    Bank("4713", "MyBank2","MyBank2", Instant.now(), Instant.now(), Instant.now(),11,"1000")
  )

  val testLayer = ZLayer.make[BankRepository](
    BankRepositoryImpl.live,
    PostgresContainer.connectionPoolConfigLayer,
    ConnectionPool.live,
    PostgresContainer.createContainer
  )

  override def spec =
    suite("Bank repository test with postgres test container")(
      test("insert two new banks") {
        for {
          oneRow <- BankRepository.create(banks)
          count <- BankRepository.list(company).runCount
        } yield assertTrue(oneRow == 2) && assertTrue(count == 4L)
      },
      test("get a Bank by its id") {
        for {
          stmt <- BankRepository.getBy((id,company))
        } yield assertTrue(stmt.name == name) && assertTrue(stmt.id==id)
      }
    ).provideLayerShared(testLayer.orDie) @@ sequential
}
