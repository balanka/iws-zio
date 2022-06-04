package com.kabasoft.iws.repository

import zio.test._

import zio.test.Assertion._
import zio.test.TestAspect._
import com.kabasoft.iws.domain.BankStatement
import java.time.Instant
import zio.ZLayer
import com.kabasoft.iws.repository.postgresql.PostgresContainer
import zio.sql.ConnectionPool

object BankStatementRepositoryLiveSpec extends ZIOSpecDefault {

val company ="1000"
  val id = "4711"

  val customers = List(
    BankStatement(4711, "B Mady",Instant.now(), Instant.now(),"TEST POSTING","TEST PURPOSE","B Mady"
      ,"430071244ACTNO","43007711BIC", 1000, "EUR","INFO TXT","1000","4711430IBAN",false,18 ),
    BankStatement(4712, "KABA Soft GmbH",Instant.now(), Instant.now(),"TEST POSTING","TEST PURPOSE","KABA Soft GmbH"
      ,"430000000ACTNO","43007711BIC", 1000, "EUR","INFO TXT","1000","470434300IBAN",false,18 )
  )

  val testLayer = ZLayer.make[BankStatementRepository](
    BankStatementRepositoryImpl.live,
    PostgresContainer.connectionPoolConfigLayer,
    ConnectionPool.live,
    PostgresContainer.createContainer
  )

  override def spec =
    suite("Bankstatement repository test with postgres test container")(
      test("count all bankstatements") {
        for {
          count <- BankStatementRepository.list(company).runCount
        } yield assert(count)(equalTo(1L))
      },
      test("insert two new bankstatements") {
        for {
          oneRow <- BankStatementRepository.create(customers)
          count <- BankStatementRepository.list(company).runCount
        } yield assert(oneRow)(equalTo(2)) && assert(count)(equalTo(3L))
      },
      test("get a BankStatement by its id") {
        for {
          stmt <- BankStatementRepository.getBy(id,company)
        } yield assert(stmt.depositor)(equalTo("B Mady")) && assert(stmt.id)(equalTo(id.toLong))
      }
    ).provideCustomLayerShared(testLayer.orDie) @@ sequential
}
