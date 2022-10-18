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
      test("insert two new transactions") {
        for {
          oneRow <- TransactionRepository.create(ftr1)
          count <- TransactionRepository.list(company).runCount
        } yield assertTrue(oneRow==3) && assertTrue(count==2)
      }
      /*, test("get a transaction by its id") {
        for {
          stmt <- TransactionRepository.getByTransId(transactionId, company)
        } yield  assertTrue(stmt.tid==transactionId)
      },


      test("Update a transaction") {
        //val terms ="changed text"
        for {
          oneRow <- TransactionRepository.create(ftr1)
          tr <- TransactionRepository.getByTransId(ftr1.tid, company)
         // nrUpdated <- TransactionRepository.modify(tr.copy(text=terms))
         // tr2 <- TransactionRepository.getByTransId(ftr1.tid, company)
        } yield   assertTrue(tr.tid==transactionId)&& assertTrue(oneRow==3)
        // && assertTrue(tr.tid==transactionId)&& assertTrue(nrUpdated==3)

      }
     */

    ).provideLayerShared(testLayer.orDie) @@ sequential
}
