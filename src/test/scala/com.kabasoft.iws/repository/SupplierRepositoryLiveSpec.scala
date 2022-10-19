package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.SupplierBuilder.suppliers
import com.kabasoft.iws.domain.AccountBuilder.company
import com.kabasoft.iws.repository.postgresql.PostgresContainer
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
val supplierId1 = "70034"
val  nameSupplie1 = "Sonstige GWG Lieferenten"
val newName = "New Supplier name"
  override def spec =
    suite("Supplier repository test with postgres test container")(
      test("insert two new supplier, modify one and look it up by id") {
        for {
          oneRow <- SupplierRepository.create(suppliers)
          count <- SupplierRepository.list(company).runCount
          stmt <- SupplierRepository.getBy(supplierId1, company)
          updated <- SupplierRepository.modify(stmt.copy(name = newName))
          stmt2 <- SupplierRepository.getBy(supplierId1, company)
        } yield assertTrue(oneRow == 2) && assertTrue(count == 6)&&
          assertTrue(stmt.name == nameSupplie1) &&
          assertTrue(stmt2.name == newName) &&
          assertTrue(stmt.id == supplierId1)
      },
    ).provideLayerShared(testLayer.orDie) @@ sequential
}
