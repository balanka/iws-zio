package com.kabasoft.iws.repository

import com.kabasoft.iws.config.appConfig
import com.kabasoft.iws.domain.AccountBuilder.companyId
import com.kabasoft.iws.domain.FinancialsTransactionBuilder.{ftr1, ftr2, modelid2}
import com.kabasoft.iws.repository.container.PostgresContainer.appResourcesL
import zio.ZLayer
import zio.test.TestAspect.*
import zio.test.*

object FinancialsTransactionRepositoryLiveSpec extends ZIOSpecDefault {

  val testLayer = ZLayer.make[FinancialsRepository & AccountRepository](
    appResourcesL.project(_.postgres),
    appConfig,
    AccountRepositoryLive.live,
    FinancialsRepositoryLive.live,
    //PostgresContainer.createContainer
  )
  override def spec =
    suite("Transaction repository test with postgres test container")(
      test("clear financials transaction") {
        for
          ftr1D <- FinancialsRepository.all(ftr1.modelid, companyId)
          ftr2D <- FinancialsRepository.all(ftr2.modelid, companyId)
          deleted <- FinancialsRepository.deleteAll(ftr1D++ftr2D)
        yield  assertTrue(deleted == ftr1D.size+ftr1D.flatMap(_.lines).size+ftr2D.size+ftr2D.flatMap(_.lines).size)
      },
      test("insert a  transactions, modify, gets a transaction by transId and count the Nr all transactions ") {
        val terms ="changed text"
        val list =List(ftr1, ftr2)
        for {
          oneRow <- FinancialsRepository.create(list)
          all <- FinancialsRepository.all(ftr1.modelid, companyId)
          ftr = all.headOption.getOrElse(ftr1)
          count <- FinancialsRepository.all(ftr1.modelid, companyId).map(_.size)
          nrUpdatedLines <- FinancialsRepository.modify(ftr1.copy(lines=ftr1.lines.map(l=>l.copy(text="Modified"))))
          nrUpdated <- FinancialsRepository.modify(ftr.copy(text=terms))
          ftr2 <- FinancialsRepository.getByTransId((ftr.id, companyId))
          ftrByModelIdCount <- FinancialsRepository.getByModelId((modelid2, companyId)).map(_.size)
        } yield assertTrue(oneRow.size == 5,  count == 1, ftrByModelIdCount == 1 , nrUpdatedLines.id == ftr.id, nrUpdated.id == ftr.id, ftr2.text == terms)
      }
    ).provideLayerShared(testLayer) @@ sequential
}