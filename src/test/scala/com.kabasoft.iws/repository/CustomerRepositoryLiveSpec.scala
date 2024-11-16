package com.kabasoft.iws.repository

import com.kabasoft.iws.config.appConfig
import com.kabasoft.iws.domain.Customer
import com.kabasoft.iws.domain.CustomerBuilder.{IbanCustomer3, customerId3, customers, nameCustomer3}
import com.kabasoft.iws.domain.AccountBuilder.companyId
import com.kabasoft.iws.repository.container.PostgresContainer
import com.kabasoft.iws.repository.container.PostgresContainer.appResourcesL
import zio.ZLayer
import zio.test.TestAspect.*
import zio.test.*


object CustomerRepositoryLiveSpec extends ZIOSpecDefault {

  val testLayer = ZLayer.make[CustomerRepository](
    appResourcesL.project(_.postgres),
    appConfig,
    BankAccountRepositoryLive.live,
    CustomerRepositoryLive.live,
    //PostgresContainer.createContainer
  )
  val  nameCustomer1 = "KKM AG"
  val newName = "New CustomerName"
  override def spec =
    suite("Customer repository test with postgres test container")(
      test("insert two new customers, modify one and look it up by id") {
        for {
          oneRow <- CustomerRepository.create(customers)
         // customers <- CustomerRepository.all(companyId)
          stmt <- CustomerRepository.getById((customerId3, Customer.MODELID, companyId))
          updated <- CustomerRepository.modify(stmt.copy(name = newName))
          stmt2 <- CustomerRepository.getById((customerId3, Customer.MODELID, companyId))
          stmt3 <- CustomerRepository.getByIban(IbanCustomer3, Customer.MODELID, companyId)
        } yield assertTrue(oneRow == 2) && //assertTrue(customers.size == 2)&&
          assertTrue(stmt.name == nameCustomer3) &&
          assertTrue(stmt2.name == newName) &&
          assertTrue(stmt.id == customerId3) &&
          assertTrue(stmt3.id == customerId3) &&
          assertTrue(updated == 1)
      },
    ).provideLayerShared(testLayer) @@ sequential
}
