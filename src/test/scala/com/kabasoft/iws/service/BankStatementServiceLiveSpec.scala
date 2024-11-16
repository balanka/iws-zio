package com.kabasoft.iws.service

import com.kabasoft.iws.config.appConfig
import com.kabasoft.iws.domain.AccountBuilder.companyId
import com.kabasoft.iws.domain.{BankStatement, BankStatementBuilder}
import com.kabasoft.iws.repository.container.PostgresContainer
import com.kabasoft.iws.repository.container.PostgresContainer.appResourcesL
import com.kabasoft.iws.repository.{AccountRepositoryLive, BankAccountRepositoryLive, BankStatementRepository, BankStatementRepositoryLive, CompanyRepository, CompanyRepositoryLive, CustomerRepository, CustomerRepositoryLive, FinancialsTransactionRepository, FinancialsTransactionRepositoryLive, SupplierRepository, SupplierRepositoryLive, VatRepositoryLive}
import zio.ZLayer
import zio.test.TestAspect.*
import zio.test.*

object BankStatementServiceLiveSpec extends ZIOSpecDefault {

  val testServiceLayer = ZLayer.make[BankStatementService& FinancialsTransactionRepository
    & CustomerRepository & SupplierRepository& CompanyRepository& BankStatementRepository](
    appResourcesL.project(_.postgres),
    appConfig,
    CustomerRepositoryLive.live,
    SupplierRepositoryLive.live,
    CompanyRepositoryLive.live,
    FinancialsTransactionRepositoryLive.live,
    VatRepositoryLive.live,
    AccountRepositoryLive.live,
    BankAccountRepositoryLive.live,
    BankStatementRepositoryLive.live,
    BankStatementServiceLive.live,
    //PostgresContainer.createContainer
  )

  override def spec =
    suite("Bank statement service  test with postgres test container")(
      test("create,ge all bank stmt  and post all bank statement"){
        for {
          created <- BankStatementRepository.create(BankStatementBuilder.bs)
          bs <- BankStatementRepository.all(BankStatement.MODELID, companyId)
          postedBS <- BankStatementService.post(bs.map(_.id), companyId).map(_.size)
        } yield  assertTrue(created == 2) && assertTrue(postedBS == 4)
     }
    ).provideLayerShared(testServiceLayer) @@ sequential
}

