package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.Account
import com.kabasoft.iws.repository.postgresql.PostgresContainer
import zio.ZLayer
import zio.sql.ConnectionPool
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test._

import java.time.Instant

object AccountRepositoryLiveSpec extends ZIOSpecDefault {

 val company ="1000"
  val id = "4712"
  val name = "MyAccount"

  val accounts = List(
    Account(id, name,"MyAccount", Instant.now(), Instant.now(), Instant.now()
      , "1000", 9, "", false, false, "EUR", BigDecimal(0), BigDecimal(0), BigDecimal(0), BigDecimal(0), Nil.toSet),
    Account("4713", "MyAccount2","MyAccount2", Instant.now(), Instant.now(), Instant.now()
      , "1000", 9, "", false, false, "EUR", BigDecimal(0), BigDecimal(0), BigDecimal(0), BigDecimal(0), Nil.toSet))


  val testLayer = ZLayer.make[AccountRepository](
    AccountRepositoryImpl.live,
    PostgresContainer.connectionPoolConfigLayer,
    ConnectionPool.live,
    PostgresContainer.createContainer
  )

  override def spec =
    suite("Account repository test with postgres test container")(
      test("count all accounts") {
        for {
          count <- AccountRepository.list(company).runCount
        } yield assert(count)(equalTo(1L))
      },
      test("insert two new accounts") {
        for {
          oneRow <- AccountRepository.create(accounts)
          count <- AccountRepository.list(company).runCount
        } yield assert(oneRow)(equalTo(2)) && assert(count)(equalTo(3L))
      },
      test("get an account by its id") {
        for {
          stmt <- AccountRepository.getBy(id,company)
        } yield assert(stmt.name)(equalTo(name)) && assert(stmt.id)(equalTo(id))
      }
    ).provideCustomLayerShared(testLayer.orDie) @@ sequential
}
