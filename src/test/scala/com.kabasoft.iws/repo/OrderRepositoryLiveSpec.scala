package com.kabasoft.iws.repository

import zio.ZLayer
import zio.test._
import zio.test.Assertion._
import com.kabasoft.iws.domain.Order
import zio.test.TestAspect._
import java.util.UUID
import java.time.LocalDate
import com.kabasoft.iws.repository.postgresql.PostgresContainer
import zio.sql.ConnectionPool

object OrderRepositoryLiveSpec extends ZIOSpecDefault {

  val testLayer = ZLayer.make[OrderRepository](
    OrderRepositoryImpl.live,
    PostgresContainer.connectionPoolConfigLayer,
    ConnectionPool.live,
    PostgresContainer.createContainer
  )

  val order = Order(UUID.randomUUID(), UUID.randomUUID(), LocalDate.now())

  def spec =
    suite("order repository test with postgres test container")(
      test("count all orders") {
        for {
          count <- OrderRepository.countAll()
        } yield assert(count)(equalTo(25))
      },
      test("insert new order") {
        for {
          _ <- OrderRepository.add(order)
          count <- OrderRepository.countAll()
        } yield assert(count)(equalTo(26))
      }
    ).provideCustomLayerShared(testLayer.orDie) @@ sequential
}
