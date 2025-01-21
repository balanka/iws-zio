package com.kabasoft.iws.repository

import com.kabasoft.iws.config.appConfig
import com.kabasoft.iws.domain.SupplierBuilder.{bankAccount, supplier1, supplierIban3, suppliers}
import com.kabasoft.iws.domain.AccountBuilder.companyId
import com.kabasoft.iws.domain.{BankAccount, Supplier}
import com.kabasoft.iws.repository.container.PostgresContainer
import com.kabasoft.iws.repository.container.PostgresContainer.appResourcesL
import zio.ZLayer
import zio.test.TestAspect.*
import zio.test.*


object SupplierRepositoryLiveSpec extends ZIOSpecDefault {

  val testLayer = ZLayer.make[SupplierRepository & BankAccountRepository](
    appResourcesL.project(_.postgres),
    appConfig,
    BankAccountRepositoryLive.live,
    SupplierRepositoryLive.live,
    //PostgresContainer.createContainer
  )
 // val supplierId1 = "70005"
  val nameSupplie1 = "Sonstige KFZ Lieferant"
  val newName = "New Supplier name"
  val ids = suppliers.map(_.id)
  val bankAccountIds = suppliers.flatMap(_.bankaccounts.map(m => (m.id, m.modelid, m.company)))

  override def spec =
    suite("Supplier repository test with postgres test container")(
      test("clear suppliers") {
        for
          deletedBankAcc <- BankAccountRepository.deleteAll(bankAccountIds)
          deleted <- SupplierRepository.deleteAll((ids, Supplier.MODELID, companyId))
        yield assertTrue(deletedBankAcc == bankAccountIds.size) && assertTrue(deleted == ids.size)
      },
      test("insert two new supplier, modify one and look it up by id") {
        for {
          createdRow <- SupplierRepository.create(suppliers)
          stmtz <- BankAccountRepository.getBy(suppliers.flatMap(_.bankaccounts.map(_.id)), BankAccount.MODEL_ID, companyId)
          count <- SupplierRepository.all(Supplier.MODELID, companyId).map(_.size)
          stmt <- SupplierRepository.getBy(suppliers.map(_.id), Supplier.MODELID, companyId)
          stmt2 <- SupplierRepository.getById((supplier1.id, Supplier.MODELID, companyId))
          updated <- SupplierRepository.modify(stmt2.copy(name = newName))
          updated2 <- SupplierRepository.modify(stmt.map(_.copy(name = newName)))
          stmt3 <- SupplierRepository.getById((supplier1.id, Supplier.MODELID, companyId))
          //stmt4 <- SupplierRepository.getByIban((bankAccount.id, Supplier.MODELID, companyId))
        } yield assertTrue(createdRow == 4) &&
          assertTrue(count == 2) &&
          assertTrue(stmt.size == 2) &&
          assertTrue(stmtz.size == 2 && stmtz.map(_.owner).distinct.headOption.getOrElse("") == supplier1.id) &&
          assertTrue(updated == 1) &&
          assertTrue(updated2 == 2) &&
          assertTrue(stmt3.name == newName) //&&
         // assertTrue(stmt4 == supplier1)
      },
    ).provideLayerShared(testLayer) @@ sequential
}