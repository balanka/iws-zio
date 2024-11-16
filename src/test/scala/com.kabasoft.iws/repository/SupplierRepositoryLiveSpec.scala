package com.kabasoft.iws.repository

import com.kabasoft.iws.config.appConfig
import com.kabasoft.iws.domain.SupplierBuilder.{supplierIban3, suppliers}
import com.kabasoft.iws.domain.AccountBuilder.companyId
import com.kabasoft.iws.domain.Supplier
import com.kabasoft.iws.repository.container.PostgresContainer
import com.kabasoft.iws.repository.container.PostgresContainer.appResourcesL
import zio.ZLayer
import zio.test.TestAspect.*
import zio.test.*


object SupplierRepositoryLiveSpec extends ZIOSpecDefault {

  val testLayer = ZLayer.make[SupplierRepository](
    appResourcesL.project(_.postgres),
    appConfig,
    BankAccountRepositoryLive.live,
    SupplierRepositoryLive.live,
    //PostgresContainer.createContainer
  )
  val supplierId1 = "70005"
  val  nameSupplie1 = "Sonstige KFZ Lieferant"
  val newName = "New Supplier name"
  override def spec =
    suite("Supplier repository test with postgres test container")(
      test("insert two new supplier, modify one and look it up by id") {
        for {
          oneRow <- SupplierRepository.create(suppliers)
          count <- SupplierRepository.all(Supplier.MODELID, companyId).map(_.size)
          stmt <- SupplierRepository.getById((supplierId1, Supplier.MODELID, companyId))
          updated <- SupplierRepository.modify(stmt.copy(name = newName))
          stmt2 <- SupplierRepository.getById((supplierId1, Supplier.MODELID, companyId))
          stmt3 <- SupplierRepository.getByIban((supplierIban3, Supplier.MODELID, companyId))
        } yield assertTrue(oneRow == 2) && assertTrue(count == 6)&&
          assertTrue(stmt.name == nameSupplie1) &&
          assertTrue(stmt2.name == newName) &&
          assertTrue(stmt3.id == supplierId1)&&
          assertTrue(stmt.id == supplierId1)&&
        assertTrue(updated == 1)
      },
    ).provideLayerShared(testLayer) @@ sequential
}
