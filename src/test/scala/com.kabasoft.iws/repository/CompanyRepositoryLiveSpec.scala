package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.Company
import com.kabasoft.iws.repository.container.PostgresContainer
import zio.ZLayer
import zio.sql.ConnectionPool
import zio.test.TestAspect._
import zio.test._

object CompanyRepositoryLiveSpec extends ZIOSpecDefault {

val company1 = Company("1001", "ABC GmbH", "Word stree1 0", "55555", "FF", "Hessen", "DE", "Deutschalnd",  "info@company.com", "Partner", "+49-4722211"
    , "1800", "Iban", "TAX/Code/XXXX", "v5", "EUR", "9900", "9800", 10)
val company = Company("1000", "ABC GmbH", "Word stree1 0", "55555", "FF", "Hessen", "DE", "Deutschalnd", "info@company.com", "Partner", "+49-4722211"
  , "1800",   "Iban","TAX/Code/XXXX", "v5", "EUR" , "9900", "9800", 10)
  val companies = List(company1)

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
          count <- CompanyRepository.list(Company.MODEL_ID).runCount
        } yield assertTrue(count == 1L)
      },
      test("insert a new company") {
        for {
          oneRow <- CompanyRepository.create2(companies)
          count <- CompanyRepository.list(Company.MODEL_ID).runCount
        } yield assertTrue(oneRow ==1) && assertTrue(count == 2)
      },
      test("get a company by its id") {
        for {
          stmt <- CompanyRepository.getBy(company.id)
        } yield assertTrue(stmt.name == company.name) && assertTrue(stmt.id==company.id)
      }
    ).provideLayerShared(testLayer.orDie) @@ sequential
}
