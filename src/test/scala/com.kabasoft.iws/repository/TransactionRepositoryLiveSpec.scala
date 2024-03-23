package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AccountBuilder.companyId
import com.kabasoft.iws.domain.TransactionBuilder.{ftr1,  ftr2, modelid2}
import com.kabasoft.iws.repository.container.PostgresContainer
import zio.ZLayer
import zio.sql.ConnectionPool
import zio.test.TestAspect._
import zio.test._

object TransactionRepositoryLiveSpec extends ZIOSpecDefault {

  val testLayer = ZLayer.make[TransactionRepository with AccountRepository](
    AccountRepositoryImpl.live,
    TransactionRepositoryImpl.live,
    PostgresContainer.connectionPoolConfigLayer,
    ConnectionPool.live,
    PostgresContainer.createContainer
  )

  override def spec =
    suite("Transaction repository test with postgres test container")(
      test("insert a  transactions, modify, gets a  goodreceoving and a supplier invoice  transaction transaction by transId and count the Nr all transactions ") {
        val terms ="changed text"
        val list =List(ftr1, ftr2)

        for {
          oneRow <- TransactionRepository.create2(list)
          all <- TransactionRepository.all(companyId)
          ftr <- TransactionRepository.getByTransId((all.head.id, companyId))
          count <- TransactionRepository.all(companyId).map(_.size)
          nrUpdatedLines <- TransactionRepository.modify(ftr.copy(lines=ftr.lines.map(l=>l.copy(text="Modified"))))
          nrUpdated <- TransactionRepository.modify(ftr.copy(text=terms))
          ftr2 <- TransactionRepository.getByTransId((ftr.id, companyId))
          ftrByModelIdCount <- TransactionRepository.getByModelId((modelid2, companyId)).map(_.size)
        } yield assertTrue(oneRow == 6,  nrUpdated == 3, count == 2, nrUpdatedLines == 3, ftr2.text == terms, ftrByModelIdCount == 1)
      }
    ).provideLayerShared(testLayer.orDie) @@ sequential
}