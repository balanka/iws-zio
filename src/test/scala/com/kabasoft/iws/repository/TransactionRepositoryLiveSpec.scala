package com.kabasoft.iws.repository

import com.kabasoft.iws.config.appConfig
import com.kabasoft.iws.domain.AccountBuilder.companyId
import com.kabasoft.iws.domain.TransactionBuilder.{ftr1, ftr2, modelid2}
import com.kabasoft.iws.repository.container.PostgresContainer
import com.kabasoft.iws.repository.container.PostgresContainer.appResourcesL
import zio.ZLayer
import zio.test.TestAspect.*
import zio.test.*
import zio.*

object TransactionRepositoryLiveSpec extends ZIOSpecDefault {

  val testLayer = ZLayer.make[TransactionRepository & AccountRepository](
    appResourcesL.project(_.postgres),
    appConfig,
    AccountRepositoryLive.live,
    StockRepositoryLive.live,
    ArticleRepositoryLive.live,
    TransactionRepositoryLive.live,
    //PostgresContainer.createContainer
  )

  override def spec =
    suite("Transaction repository test with postgres test container")(
      test("clear logistical  transaction") {
        for
          ftr1D <- TransactionRepository.all(ftr1.modelid, companyId)
          ftr2D <- TransactionRepository.all(ftr2.modelid, companyId)
          deleted <- TransactionRepository.deleteAll()
        yield  assertTrue(deleted == ftr1D.size+ftr1D.flatMap(_.lines).size+ftr2D.size+ftr2D.flatMap(_.lines).size)
      },
      test("insert a logistical transactions, modify, gets a  good receiving and a supplier invoice  transaction by transId and count the Nr all transactions ") {
        val terms ="changed text"
        val list =List(ftr1, ftr2)

        for {
          oneRow <- TransactionRepository.create(list)
          all <- TransactionRepository.all(ftr1.modelid, companyId)
          ftr = all.headOption.getOrElse(ftr1)
          count <- TransactionRepository.all(ftr1.modelid, companyId).map(_.size)
          nrUpdatedLines <- TransactionRepository.modify(ftr.copy(lines=ftr.lines.map(_.copy(text="Modified"))))
          nrUpdated <- TransactionRepository.modify(ftr.copy(text=terms))
          ftr2 <- TransactionRepository.getById((ftr.id, ftr.modelid, companyId))
          ftrByModelIdCount <- TransactionRepository.getByModelId((modelid2, companyId)).map(_.size)
        } yield assertTrue(oneRow == 6,  nrUpdated == 3, count == 1, nrUpdatedLines == 3, ftr2.text == terms, ftrByModelIdCount == 1)
      }
    ).provideLayerShared(testLayer) @@ sequential
}