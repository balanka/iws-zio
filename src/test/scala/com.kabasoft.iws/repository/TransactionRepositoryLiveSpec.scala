package com.kabasoft.iws.repository

import com.kabasoft.iws.repository.common.AccountBuilder.company
import com.kabasoft.iws.repository.common.TransactionBuilder.{ftr1, transactionId}
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
        } yield assertTrue(oneRow==3) && assertTrue(count==2L)
      },
       test("get a transaction by its id") {
        for {
          stmt <- TransactionRepository.getByTransId(transactionId, company)
        } yield  assertTrue(stmt.tid==transactionId)
      },
      test("Update a transaction") {
        val terms ="changed text"
        for {
          tr <- TransactionRepository.getByTransId(transactionId, company)
          transaction= tr.copy(text=terms)
          nrUpdated <- TransactionRepository.modify(transaction)
          tr2 <- TransactionRepository.getByTransId(transaction.tid, company)
        } yield  assertTrue(tr.tid==transactionId)&& assertTrue(tr2.tid==transactionId)/*&& assertTrue(tr2.text==terms)*/&&assertTrue(nrUpdated==3)
      }
    ).provideCustomLayerShared(testLayer.orDie) @@ sequential
}
