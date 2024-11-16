package com.kabasoft.iws.repository

import com.kabasoft.iws.config.appConfig
import com.kabasoft.iws.domain.AccountBuilder.companyId
import com.kabasoft.iws.domain.TransactionBuilder.{ftr1, ftr2, modelid2}
import com.kabasoft.iws.repository.container.PostgresContainer
import com.kabasoft.iws.repository.container.PostgresContainer.appResourcesL
import zio.ZLayer
import zio.test.TestAspect.*
import zio.test.*

object TransactionRepositoryLiveSpec extends ZIOSpecDefault {

  val testLayer = ZLayer.make[TransactionRepository & AccountRepository](
    appResourcesL.project(_.postgres),
    appConfig,
    AccountRepositoryLive.live,
    TransactionRepositoryLive.live,
    //PostgresContainer.createContainer
  )

  override def spec =
    suite("Transaction repository test with postgres test container")(
      test("insert a  transactions, modify, gets a  good receiving and a supplier invoice  transaction by transId and count the Nr all transactions ") {
        val terms ="changed text"
        val list =List(ftr1, ftr2)

        for {
          oneRow <- TransactionRepository.create(list)
          all <- TransactionRepository.all(ftr1.modelid, companyId).map(_.size)
          ftr <- TransactionRepository.getById((ftr1.id, ftr1.modelid, companyId))
          count <- TransactionRepository.all(ftr1.modelid, companyId).map(_.size)
          nrUpdatedLines <- TransactionRepository.modify(ftr.copy(lines=ftr.lines.map(l=>l.copy(text="Modified"))))
          nrUpdated <- TransactionRepository.modify(ftr.copy(text=terms))
          ftr2 <- TransactionRepository.getById((ftr.id, ftr.modelid, companyId))
          ftrByModelIdCount <- TransactionRepository.getByModelId((modelid2, companyId)).map(_.size)
        } yield assertTrue(oneRow == 6,  all ==5, nrUpdated == 3, count == 2, nrUpdatedLines == 3, ftr2.text == terms, ftrByModelIdCount == 1)
      }
    ).provideLayerShared(testLayer) @@ sequential
}