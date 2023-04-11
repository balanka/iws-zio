package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AccountBuilder.companyId
import com.kabasoft.iws.domain.TransactionBuilder.{ftr1, ftr2, modelid2}
import com.kabasoft.iws.repository.container.PostgresContainer
import zio.ZLayer
import zio.sql.ConnectionPool
import zio.test.TestAspect._
import zio.test._

object TransactionRepositoryLiveSpec extends ZIOSpecDefault {

  val testLayer = ZLayer.make[TransactionRepository](
    TransactionRepositoryImpl.live,
    PostgresContainer.connectionPoolConfigLayer,
    ConnectionPool.live,
    PostgresContainer.createContainer
  )

  override def spec =
    suite("Transaction repository test with postgres test container")(
      test("insert a new transactions, modify, gets aTransaction by transId and count the Nr all transactions ") {
        val terms ="changed text"
        for {
          oneRow <- TransactionRepository.create(List(ftr1,ftr2) )
          all <- TransactionRepository.all(companyId)
          ftr <- TransactionRepository.getByTransId((all(0).id, companyId))
          count <- TransactionRepository.all(companyId).map(_.size)
          nrUpdated <- TransactionRepository.modify(ftr.copy(text=terms))
          ftr2 <- TransactionRepository.getByTransId((ftr.id, companyId))
          ftrByModelIdCount <- TransactionRepository.getByModelId((modelid2, companyId)).map(_.size)
        } yield assertTrue(oneRow == 5)  && assertTrue(count == 3) && assertTrue(ftrByModelIdCount == 1) &&
          assertTrue(nrUpdated == 3) &&assertTrue(ftr2.text == terms)
      }
    ).provideLayerShared(testLayer.orDie) @@ sequential
}