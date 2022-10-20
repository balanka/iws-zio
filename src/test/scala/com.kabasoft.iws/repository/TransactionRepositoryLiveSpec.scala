package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AccountBuilder.company
import com.kabasoft.iws.domain.TransactionBuilder.ftr1
import com.kabasoft.iws.repository.postgresql.PostgresContainer
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
          oneRow <- TransactionRepository.create(ftr1)
          ftr <- TransactionRepository.getByTransId(ftr1.tid, company)
          count <- TransactionRepository.list(company).runCount
          nrUpdated <- TransactionRepository.modify(ftr.copy(text=terms))
          ftr2 <- TransactionRepository.getByTransId(ftr1.tid, company)
        } yield assertTrue(oneRow==3) && assertTrue(ftr.tid==ftr1.tid) && assertTrue(count==2) &&
         assertTrue(ftr2.text==terms)
      }
    ).provideLayerShared(testLayer.orDie) @@ sequential
}
