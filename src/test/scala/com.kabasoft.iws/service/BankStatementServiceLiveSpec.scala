package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AccountBuilder.company
import com.kabasoft.iws.domain.BankStatementBuilder
import com.kabasoft.iws.repository.postgresql.PostgresContainer
import com.kabasoft.iws.repository.{BankStatementRepository, BankStatementRepositoryImpl, CompanyRepository, CompanyRepositoryImpl, CustomerRepository, CustomerRepositoryImpl, SupplierRepository, SupplierRepositoryImpl, TransactionRepository, TransactionRepositoryImpl}
import zio.ZLayer
import zio.sql.ConnectionPool
import zio.test.TestAspect._
import zio.test._

object BankStatementServiceLiveSpec extends ZIOSpecDefault {

  val testServiceLayer = ZLayer.make[BankStatementService with TransactionRepository
    with CustomerRepository with SupplierRepository with CompanyRepository with BankStatementRepository](
    CustomerRepositoryImpl.live,
    SupplierRepositoryImpl.live,
    CompanyRepositoryImpl.live,
    TransactionRepositoryImpl.live,
    BankStatementRepositoryImpl.live,
    BankStatementServiceImpl.live,
    PostgresContainer.connectionPoolConfigLayer,
    ConnectionPool.live,
    PostgresContainer.createContainer
  )

  override def spec =
    suite("Bank statement service  test with postgres test container")(
      test("create,ge all bank stmt  and post all bank statement"){
        for {
          created <- BankStatementRepository.create(BankStatementBuilder.bs)
          bs <- BankStatementRepository.list(company).runCollect.map(_.toList)
          postedBS <- BankStatementService.postAll(bs.map(_.id), company)
        } yield  assertTrue(created == 2) && assertTrue(postedBS == 8)
     }
    ).provideLayerShared(testServiceLayer.orDie) @@ sequential
}

