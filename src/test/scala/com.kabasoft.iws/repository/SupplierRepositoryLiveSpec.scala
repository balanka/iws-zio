package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.SupplierBuilder.{supplierIban3, suppliers}
import com.kabasoft.iws.domain.AccountBuilder.companyId
import com.kabasoft.iws.repository.container.PostgresContainer
import zio.ZLayer
import zio.sql.ConnectionPool
import zio.test.TestAspect._
import zio.test._


object SupplierRepositoryLiveSpec extends ZIOSpecDefault {

  val testLayer = ZLayer.make[SupplierRepository](
    SupplierRepositoryImpl.live,
    PostgresContainer.connectionPoolConfigLayer,
    ConnectionPool.live,
    PostgresContainer.createContainer
  )
val supplierId1 = "70005"
val  nameSupplie1 = "Sonstige KFZ Lieferant"
val newName = "New Supplier name"
  override def spec =
    suite("Supplier repository test with postgres test container")(
      test("insert two new supplier, modify one and look it up by id") {
        for {
          oneRow <- SupplierRepository.create(suppliers)
          count <- SupplierRepository.list(companyId).runCount
          stmt <- SupplierRepository.getBy(supplierId1, companyId)
          updated <- SupplierRepository.modify(stmt.copy(name = newName))
          stmt2 <- SupplierRepository.getBy(supplierId1, companyId)
          stmt3 <- SupplierRepository.getByIban(supplierIban3, companyId)
        } yield assertTrue(oneRow == 2) && assertTrue(count == 8)&&
          assertTrue(stmt.name == nameSupplie1) &&
          assertTrue(stmt2.name == newName) &&
          assertTrue(stmt3.id == supplierId1)&&
          assertTrue(stmt.id == supplierId1)&&
        assertTrue(updated == 1)
      },
    ).provideLayerShared(testLayer.orDie) @@ sequential
}
