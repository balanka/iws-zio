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
      test("insert a  transactions, modify, gets a transaction by transId and count the Nr all transactions ") {
        val terms ="changed text"
        for {
          oneRow <- TransactionRepository.create(List(ftr1,ftr2)).map(_.map(_.lines.size).sum+2)
          all <- TransactionRepository.all(companyId)
          ftr <- TransactionRepository.getByTransId((all(0).id, companyId))
          count <- TransactionRepository.all(companyId).map(_.size)
          nrUpdatedLines <- TransactionRepository.modify(ftr1.copy(lines=ftr1.lines.map(l=>l.copy(text="Modified"))))
          nrUpdated <- TransactionRepository.modify(ftr.copy(text=terms))
          ftr2 <- TransactionRepository.getByTransId((ftr.id, companyId))
          ftrByModelIdCount <- TransactionRepository.getByModelId((modelid2, companyId)).map(_.size)
        } yield assertTrue(oneRow == 7, count == 3, ftrByModelIdCount == 1, nrUpdated == 3, ftr2.text == terms, nrUpdatedLines == 3)
      }
    ).provideLayerShared(testLayer.orDie) @@ sequential
}