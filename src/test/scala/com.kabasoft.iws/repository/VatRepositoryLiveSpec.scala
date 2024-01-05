package com.kabasoft.iws.repository

import zio.test._
import zio.test.TestAspect._
import com.kabasoft.iws.domain.Vat

import java.time.Instant
import zio.ZLayer
import com.kabasoft.iws.repository.container.PostgresContainer
import zio.sql.ConnectionPool

import java.math.{BigDecimal, RoundingMode}

object VatRepositoryLiveSpec extends ZIOSpecDefault {

 val company ="1000"
  val id = "4712"
  val name = "MyVat2"
  val vat = new BigDecimal("0.19").setScale(2, RoundingMode.UNNECESSARY)
  val vat1 = new BigDecimal("0.07").setScale(2, RoundingMode.UNNECESSARY)

  val vats = List(
    Vat(id, name,"MyVat2",vat, "1406", "3806", Instant.now(), Instant.now(), Instant.now(), "1000", 6),
    Vat("4713", "MyBank2","MyBank2", vat1, "1406", "3806", Instant.now(), Instant.now(), Instant.now(), "1000", 6)
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
          count <- VatRepository.list((Vat.MODEL_ID, company)).runCount
        } yield assertTrue(count==2L)
      },
      test("insert two new vats") {
        for {
          oneRow <- VatRepository.create2(vats)
          count <- VatRepository.list((Vat.MODEL_ID, company)).runCount
        } yield assertTrue(oneRow==2) && assertTrue(count==4L)
      },
      test("get a Vat by its id") {
        for {
          stmt <- VatRepository.getBy((id,company))
        } yield assertTrue(stmt.name==name) && assertTrue(stmt.id==id)
      }
    ).provideLayerShared(testLayer.orDie) @@ sequential
}
