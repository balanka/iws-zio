package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AccountBuilder.companyId
import com.kabasoft.iws.domain.BankStatementBuilder
import com.kabasoft.iws.repository.container.PostgresContainer
import com.kabasoft.iws.repository.{AccountRepositoryImpl, BankStatementRepository, BankStatementRepositoryImpl, CompanyRepository, CompanyRepositoryLive, CustomerRepository, CustomerRepositoryLive, SupplierRepository, SupplierRepositoryImpl, FinancialsTransactionRepository, FinancialsTransactionRepositoryImpl, VatRepositoryImpl}
import zio.ZLayer
import zio.sql.ConnectionPool
import zio.test.TestAspect._
import zio.test._

object BankStatementServiceLiveSpec extends ZIOSpecDefault {

  val testServiceLayer = ZLayer.make[BankStatementService with FinancialsTransactionRepository
    with CustomerRepository with SupplierRepository with CompanyRepository with BankStatementRepository](
    CustomerRepositoryLive.live,
    SupplierRepositoryImpl.live,
    CompanyRepositoryLive.live,
    FinancialsTransactionRepositoryImpl.live,
    VatRepositoryImpl.live,
    AccountRepositoryImpl.live,
    BankStatementRepositoryImpl.live,
    BankStatementServiceLive.live,
    PostgresContainer.connectionPoolConfigLayer,
    ConnectionPool.live,
    PostgresContainer.createContainer
  )

  override def spec =
    suite("Bank statement service  test with postgres test container")(
      test("create,ge all bank stmt  and post all bank statement"){
        for {
          created <- BankStatementRepository.create2(BankStatementBuilder.bs)
          bs <- BankStatementRepository.list( companyId).runCollect.map(_.toList)
          postedBS <- BankStatementService.post(bs.map(_.id), companyId).map(_.size)
        } yield  assertTrue(created == 2) && assertTrue(postedBS == 4)
     }
    ).provideLayerShared(testServiceLayer.orDie) @@ sequential
}

