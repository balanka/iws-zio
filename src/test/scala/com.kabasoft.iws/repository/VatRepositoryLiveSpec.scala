package com.kabasoft.iws.repository

import zio.test._

import zio.test.TestAspect._
import com.kabasoft.iws.domain.Vat
import java.time.Instant
import zio.ZLayer
import com.kabasoft.iws.repository.postgresql.PostgresContainer
import zio.sql.ConnectionPool

object VatRepositoryLiveSpec extends ZIOSpecDefault {

 val company ="1000"
  val id = "4712"
  val name = "MyVat2"

  val vats = List(
    Vat(id, name,"MyVat2",2, "1406", "3806", Instant.now(), Instant.now(), Instant.now(), "1000", 6),
    Vat("4713", "MyBank2","MyBank2", 3, "1406", "3806", Instant.now(), Instant.now(), Instant.now(), "1000", 6)
  )

  val testLayer = ZLayer.make[VatRepository](
    VatRepositoryImpl.live,
    PostgresContainer.connectionPoolConfigLayer,
    ConnectionPool.live,
    PostgresContainer.createContainer
  )

  override def spec =
    suite("Vat repository test with postgres test container")(
      test("count all vats") {
        for {
          count <- VatRepository.list(company).runCount
        } yield assertTrue(count==1L)
      },
      test("insert two new vats") {
        for {
          oneRow <- VatRepository.create(vats)
          count <- VatRepository.list(company).runCount
        } yield assertTrue(oneRow==2) && assertTrue(count==3L)
      },
      test("get a Vat by its id") {
        for {
          stmt <- VatRepository.getBy(id,company)
        } yield assertTrue(stmt.name==name) && assertTrue(stmt.id==id)
      }
    ).provideLayerShared(testLayer.orDie) @@ sequential
}
