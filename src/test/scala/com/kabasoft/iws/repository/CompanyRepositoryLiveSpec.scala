package com.kabasoft.iws.repository

import com.kabasoft.iws.config.appConfig
import com.kabasoft.iws.domain.Company
import com.kabasoft.iws.repository.container.PostgresContainer
import com.kabasoft.iws.repository.container.PostgresContainer.appResourcesL
import zio.ZLayer
import zio.test.TestAspect.*
import zio.test.*

object CompanyRepositoryLiveSpec extends ZIOSpecDefault {

val company1 = Company("-1001", "ABC GmbH", "Word stree1 0", "55555", "FF", "Hessen", "DE", "Deutschalnd",  "info@company.com", "Partner", "+49-4722211"
    , "1800", "Iban", "TAX/Code/XXXX", "v5", "EUR", "9900", "9800", "33333", "44444", 10)
val company = Company("-1000", "ABC GmbH", "Word stree1 0", "55555", "FF", "Hessen", "DE", "Deutschalnd", "info@company.com", "Partner", "+49-4722211"
  , "1800",   "Iban","TAX/Code/XXXX", "v5", "EUR" , "9900", "9800", "33333", "44444", 10)
  val companies = List(company1)

  val testLayer = ZLayer.make[CompanyRepository](
    appResourcesL.project(_.postgres),
    appConfig,
    BankAccountRepositoryLive.live,
    CompanyRepositoryLive.live,
    //PostgresContainer.createContainer
  )
  val ids = companies.map(m => (m.id, m.modelid))
  override def spec =
    suite("company repository test with postgres test container")(
      test("clear banks") {
        for {
          deleted <- CompanyRepository.deleteAll(ids)
        } yield assertTrue(deleted == ids.size)
      },
      test("insert a new company") {
        for {
          oneRow <- CompanyRepository.create(companies)
          //count <- CompanyRepository.all(Company.MODEL_ID).map(_.size)
        } yield assertTrue(oneRow ==1) //&& assertTrue(count == 2)
      },
      test("get a company by its id") {
        for {
          stmt <- CompanyRepository.getById(company.id, Company.MODEL_ID)
        } yield assertTrue(stmt.name == company.name) && assertTrue(stmt.id==company.id)
      }
    ).provideLayerShared(testLayer) @@ sequential
}
