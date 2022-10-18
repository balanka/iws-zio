package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.Company
import com.kabasoft.iws.repository.postgresql.PostgresContainer
import zio.ZLayer
import zio.sql.ConnectionPool
import zio.test.TestAspect._
import zio.test._

import java.time.Instant

object CompanyRepositoryLiveSpec extends ZIOSpecDefault {

val company = Company("1000X", "ABC GmbH", "Word stree1 0", "55555", "FF", "DE", "+49-000000", "+49-0000001", "info@company.com"
  , "John", "de_DE", "1800", "XXXX/XXXX/XXXX", "v5", "EUR", Instant.now(), "9900", "9800", 10)
  val companies = List(company)

  val testLayer = ZLayer.make[CompanyRepository](
    CompanyRepositoryImpl.live,
    PostgresContainer.connectionPoolConfigLayer,
    ConnectionPool.live,
    PostgresContainer.createContainer
  )

  override def spec =
    suite("company repository test with postgres test container")(
      test("count all company") {
        for {
          count <- CompanyRepository.list(company.id).runCount
        } yield assertTrue(count == 1L)
      },
      test("insert a new company") {
        for {
          oneRow <- CompanyRepository.create(companies)
          count <- CompanyRepository.list(company.id).runCount
        } yield assertTrue(oneRow ==1) && assertTrue(count == 2)
      },
      test("get a company by its id") {
        for {
          stmt <- CompanyRepository.getBy(company.id)
        } yield assertTrue(stmt.name == company.name) && assertTrue(stmt.id==company.id)
      }
    ).provideLayerShared(testLayer.orDie) @@ sequential
}
