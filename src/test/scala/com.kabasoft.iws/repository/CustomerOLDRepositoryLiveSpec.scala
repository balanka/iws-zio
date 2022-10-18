package com.kabasoft.iws.repository

import zio.test._

import zio.test.TestAspect._
import com.kabasoft.iws.domain.Customer_OLD
import java.util.UUID
import java.time.LocalDate
import zio.ZLayer
import com.kabasoft.iws.repository.postgresql.PostgresContainer
import zio.sql.ConnectionPool

object CustomerOLDRepositoryLiveSpec extends ZIOSpecDefault {

  val customerId1 = UUID.randomUUID()

  val customers = List(
    Customer_OLD(customerId1, "Peter", "Schwarz", false, LocalDate.now()),
    Customer_OLD(UUID.randomUUID(), "Laszlo", "Wider", true, LocalDate.now())
  )

  val testLayer = ZLayer.make[CustomerOLDRepository](
    CustomerOLDRepositoryImpl.live,
    PostgresContainer.connectionPoolConfigLayer,
    ConnectionPool.live,
    PostgresContainer.createContainer
  )

  override def spec =
    suite("customer repository test with postgres test container")(
      test("count all customers") {
        for {
          count <- CustomerOLDRepository.findAll().runCount
        } yield assertTrue(count == 5L)
      },
      test("insert two new customers") {
        for {
          oneRow <- CustomerOLDRepository.add(customers)
          count <- CustomerOLDRepository.findAll().runCount
        } yield assertTrue(oneRow==2) && assertTrue(count==7L)
      },
      test("get inserted customer") {
        for {
          customer <- CustomerOLDRepository.findById(customerId1)
        } yield assertTrue(customer.fname=="Peter")
      }
    ).provideLayerShared(testLayer.orDie) @@ sequential
}
