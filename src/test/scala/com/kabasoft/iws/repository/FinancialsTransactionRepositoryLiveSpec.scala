package com.kabasoft.iws.repository

import com.kabasoft.iws.config.appConfig
import com.kabasoft.iws.domain.AccountBuilder.companyId
import com.kabasoft.iws.domain.FinancialsTransactionBuilder.{ftr1, ftr2, modelid2}
import com.kabasoft.iws.repository.container.PostgresContainer
import com.kabasoft.iws.repository.container.PostgresContainer.appResourcesL
import zio.ZLayer
import zio.test.TestAspect.*
import zio.test.*

object FinancialsTransactionRepositoryLiveSpec extends ZIOSpecDefault {

  val testLayer = ZLayer.make[FinancialsTransactionRepository & AccountRepository](
    appResourcesL.project(_.postgres),
    appConfig,
    AccountRepositoryLive.live,
    FinancialsTransactionRepositoryLive.live,
    //PostgresContainer.createContainer
  )
  override def spec =
    suite("Transaction repository test with postgres test container")(
      test("clear financials transaction") {
        for
          ftr1D <- FinancialsTransactionRepository.all(ftr1.modelid, companyId)
          ftr2D <- FinancialsTransactionRepository.all(ftr2.modelid, companyId)
          deleted <- FinancialsTransactionRepository.deleteAll(ftr1D++ftr2D)
        yield  assertTrue(deleted == ftr1D.size+ftr1D.flatMap(_.lines).size+ftr2D.size+ftr2D.flatMap(_.lines).size)
      },
      test("insert a  transactions, modify, gets a transaction by transId and count the Nr all transactions ") {
        val terms ="changed text"
        val list =List(ftr1, ftr2)
        for {
          oneRow <- FinancialsTransactionRepository.create(list)
          all <- FinancialsTransactionRepository.all(ftr1.modelid, companyId)
          ftr = all.headOption.getOrElse(ftr1)
          count <- FinancialsTransactionRepository.all(ftr1.modelid, companyId).map(_.size)
          nrUpdatedLines <- FinancialsTransactionRepository.modify(ftr1.copy(lines=ftr1.lines.map(l=>l.copy(text="Modified"))))
          nrUpdated <- FinancialsTransactionRepository.modify(ftr.copy(text=terms))
          ftr2 <- FinancialsTransactionRepository.getByTransId((ftr.id, companyId))
          ftrByModelIdCount <- FinancialsTransactionRepository.getByModelId((modelid2, companyId)).map(_.size)
        } yield assertTrue(oneRow == 5,  count == 1, ftrByModelIdCount == 1 , nrUpdatedLines == 3, nrUpdated == 3, ftr2.text == terms)
      }
    ).provideLayerShared(testLayer) @@ sequential
}