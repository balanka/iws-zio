package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.CustomerBuilder.{IbanCustomer3, customerId3, customers, nameCustomer3}
import com.kabasoft.iws.domain.AccountBuilder.companyId
import com.kabasoft.iws.repository.container.PostgresContainer
import zio.ZLayer
import zio.sql.ConnectionPool
import zio.test.TestAspect._
import zio.test._


object CustomerRepositoryLiveSpec extends ZIOSpecDefault {

  val testLayer = ZLayer.make[CustomerRepository](
    CustomerRepositoryImpl.live,
    PostgresContainer.connectionPoolConfigLayer,
    ConnectionPool.live,
    PostgresContainer.createContainer
  )
val  nameCustomer1 = "KKM AG"
val newName = "New CustomerName"
  override def spec =
    suite("Customer repository test with postgres test container")(
      test("insert two new customers, modify one and look it up by id") {
        for {
          oneRow <- CustomerRepository.create(customers)
         // customers <- CustomerRepository.all(companyId)
          stmt <- CustomerRepository.getBy((customerId3, companyId))
          updated <- CustomerRepository.modify(stmt.copy(name = newName))
          stmt2 <- CustomerRepository.getBy((customerId3, companyId))
          stmt3 <- CustomerRepository.getByIban(IbanCustomer3, companyId)
        } yield assertTrue(oneRow == 2) && //assertTrue(customers.size == 2)&&
          assertTrue(stmt.name == nameCustomer3) &&
          assertTrue(stmt2.name == newName) &&
          assertTrue(stmt.id == customerId3) &&
          assertTrue(stmt3.id == customerId3) &&
          assertTrue(updated == 1)
      },
    ).provideLayerShared(testLayer.orDie) @@ sequential
}
