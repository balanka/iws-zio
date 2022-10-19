package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.CustomerBuilder.{customerId2, customers }
import com.kabasoft.iws.domain.AccountBuilder.company
import com.kabasoft.iws.repository.postgresql.PostgresContainer
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
val customerId1 = "5004"
val  nameCustomer1 = "KKM AG"
val newName = "New CustomerName"
  override def spec =
    suite("Customer repository test with postgres test container")(
      test("insert two new customers, modify one and look it up by id") {
        for {
          oneRow <- CustomerRepository.create(customers)
          count <- CustomerRepository.list(company).runCount
          stmt <- CustomerRepository.getBy(customerId1, company)
          updated <- CustomerRepository.modify(stmt.copy(name = newName))
          stmt2 <- CustomerRepository.getBy(customerId1, company)
        } yield assertTrue(oneRow == 2) && assertTrue(count == 2)&&
          assertTrue(stmt.name == nameCustomer1) &&
          assertTrue(stmt2.name == newName) &&
          assertTrue(stmt.id == customerId1)
      },
    ).provideLayerShared(testLayer.orDie) @@ sequential
}
