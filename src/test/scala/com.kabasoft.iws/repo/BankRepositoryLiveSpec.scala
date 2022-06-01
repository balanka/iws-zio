package com.kabasoft.iws.repository

import zio.test._

import zio.test.Assertion._
import zio.test.TestAspect._
import com.kabasoft.iws.domain.Bank
import java.time.Instant
import zio.ZLayer
import com.kabasoft.iws.repository.postgresql.PostgresContainer
import zio.sql.ConnectionPool

object BankRepositoryLiveSpec extends ZIOSpecDefault {

 val company ="1000"
  val id = "4712"
  val name = "MyBank"

  val customers = List(
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
      test("count all banks") {
        for {
          count <- BankRepository.list(company).runCount
        } yield assert(count)(equalTo(1L))
      },
      test("insert two new banks") {
        for {
          oneRow <- BankRepository.create(customers)
          count <- BankRepository.list(company).runCount
        } yield assert(oneRow)(equalTo(2)) && assert(count)(equalTo(3L))
      },
      test("get a Bank by its id") {
        for {
          stmt <- BankRepository.getBy(id,company)
        } yield assert(stmt.name)(equalTo(name)) && assert(stmt.id)(equalTo(id))
      }
    ).provideCustomLayerShared(testLayer.orDie) @@ sequential
}
