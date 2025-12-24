package com.kabasoft.iws.repository

import com.kabasoft.iws.config.appConfig
import com.kabasoft.iws.domain.{BankAccount, Customer}
import com.kabasoft.iws.domain.CustomerBuilder.{IbanCustomer3, bankAccount, bankAccountId, customer1, customerId1, customerId2, customerId3, customers, nameCustomer1, nameCustomer3}
import com.kabasoft.iws.domain.AccountBuilder.companyId
import com.kabasoft.iws.repository.container.PostgresContainer
import com.kabasoft.iws.repository.container.PostgresContainer.appResourcesL
import zio.ZLayer
import zio.test.TestAspect.*
import zio.test.*


object CustomerRepositoryLiveSpec extends ZIOSpecDefault {

  val testLayer = ZLayer.make[CustomerRepository & BankAccountRepository](
    appResourcesL.project(_.postgres),
    appConfig,
    BankAccountRepositoryLive.live,
    CustomerRepositoryLive.live,
    //PostgresContainer.createContainer
  )
  //val  nameCustomer1 = "KKM AG"
  val newName = "New CustomerName"
  val ids = customers.map(m => (m.id, m.modelid, m.company))
  val bankAccountIds = customers.flatMap(_.bankaccounts.map(m=> (m.id, m.modelid, m.company)))
  override def spec =
    suite("Customer repository test with postgres test container")(
      test("clear customers") {
        for {
          deletedBankAcc <- BankAccountRepository.deleteAll(bankAccountIds)
          deleted <- CustomerRepository.deleteAll(ids)
        } yield  assertTrue(deletedBankAcc == bankAccountIds.size) && assertTrue(deleted == ids.size)
       
      },
      test("insert two new customers, modify one and look it up by id") {
        for 
                createdRow <- CustomerRepository.create(customers)
                stmtz <- BankAccountRepository.getBy(customers.flatMap(_.bankaccounts.map(_.id)), BankAccount.MODEL_ID, companyId)
                stmt <- CustomerRepository.getBy(customers.map(_.id), Customer.MODELID, companyId)
                stmt2 <- CustomerRepository.getById((customer1.id, Customer.MODELID, companyId))
                updated <- CustomerRepository.modify(stmt2.copy(name = newName))
                updated2 <- CustomerRepository.modify(stmt.map(_.copy(name = newName)))
                stmt3 <- CustomerRepository.getById((customer1.id, Customer.MODELID, companyId))
                stmt4 <- CustomerRepository.getByIban((bankAccount.id, Customer.MODELID, companyId))
           yield assertTrue(createdRow == 4) &&
                 assertTrue(stmt.size == 2) &&
                 assertTrue(stmtz.size == 2) &&
                 assertTrue(updated == 1) &&
                 assertTrue(updated2 == 2) &&
                 assertTrue(stmt3.name == newName) &&
                 assertTrue(stmt4 == stmt3)
      },
    ).provideLayerShared(testLayer) @@ sequential
}
