package com.kabasoft.iws.repository

import com.kabasoft.iws.config.appConfig
import zio.test.*
import zio.test.TestAspect.*
import com.kabasoft.iws.domain.Vat

import java.time.Instant
import zio.ZLayer
import com.kabasoft.iws.repository.container.PostgresContainer
import com.kabasoft.iws.repository.container.PostgresContainer.appResourcesL

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
    appResourcesL.project(_.postgres),
    appConfig,
    VatRepositoryLive.live,
    //PostgresContainer.createContainer
  )

  override def spec =
    suite("Vat repository test with postgres test container")(
      test("count all vats") {
        for {
          count <- VatRepository.all((Vat.MODEL_ID, company)).map(_.size)
        } yield assertTrue(count==3)
      },
      test("insert two new vats") {
        for {
          oneRow <- VatRepository.create(vats)
          count <- VatRepository.all((Vat.MODEL_ID, company)).map(_.size)
        } yield assertTrue(oneRow==2) && assertTrue(count==3)
      },
      test("get a Vat by its id") {
        for {
          stmt <- VatRepository.getById((id, Vat.MODEL_ID, company))
        } yield assertTrue(stmt.name==name) && assertTrue(stmt.id==id)
      }
    ).provideLayerShared(testLayer) @@ sequential
}
