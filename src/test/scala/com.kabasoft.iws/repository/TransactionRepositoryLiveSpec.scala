package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.{Account, FinancialsTransaction, FinancialsTransactionDetails, common}
import com.kabasoft.iws.repository.postgresql.PostgresContainer
import zio.ZLayer
import zio.sql.ConnectionPool
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test._

import java.time.Instant

object TransactionRepositoryLiveSpec extends ZIOSpecDefault {

 val company ="1000"
  val paccountId = "9999"
  val accountId = "4712"
  val oaccountId = "4713"
  val oaccountId2 = "4714"
  val transactionId = 4711L
  val lineTransactionId1 = 1L
  val lineTransactionId2 = 2L
  val name = "MyAccount"
  val modelid = 112
  val period = common.getPeriod(Instant.now())
  val side = true
  val amount = BigDecimal(100)
  val vat = 0.19
  val vatAmount = BigDecimal(100)*vat
  val terms = "terms"
  val currency = "EUR"

  val paccount =  Account(paccountId, "ParentAccountName","ParentAccountDescription", Instant.now(), Instant.now(), Instant.now()
    , "1000", 9, "", true, true, "EUR", BigDecimal(0), BigDecimal(0), BigDecimal(0), BigDecimal(0), Nil.toSet)
  val account =  Account(accountId, name,"MyAccount", Instant.now(), Instant.now(), Instant.now()
      , "1000", 9, paccountId, true, true, "EUR", BigDecimal(0), BigDecimal(0), BigDecimal(0), BigDecimal(0), Nil.toSet)
  val oaccount1 =  Account(oaccountId, "MyAccount2","MyAccount2", Instant.now(), Instant.now(), Instant.now()
      , "1000", 9, paccountId, false, true, "EUR", BigDecimal(0), BigDecimal(0), BigDecimal(0), BigDecimal(0), Nil.toSet)
  val oaccount2 =  Account(oaccountId, "MyAccount2","MyAccount2", Instant.now(), Instant.now(), Instant.now()
    , "1000", 9, paccountId, false, true, "EUR", BigDecimal(0), BigDecimal(0), BigDecimal(0), BigDecimal(0), Nil.toSet)


  //val transactions = List(ftr1)

  val line1=  FinancialsTransactionDetails(lineTransactionId1, transactionId, accountId, side, oaccountId, amount-vatAmount, Instant.now(), terms, currency)
  val line2=  FinancialsTransactionDetails(lineTransactionId2, transactionId, accountId, side, oaccountId2, vatAmount, Instant.now(), terms, currency)

  val ftr1 = FinancialsTransaction(transactionId,-1,"311",accountId, Instant.now(), Instant.now(), Instant.now()
  , period, false, modelid, company, "comments", -1,-1, List(line1, line2))

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
          oneRow <- TransactionRepository.create(ftr1.toDerive())
          count <- TransactionRepository.list(company).runCount
        } yield assert(oneRow)(equalTo(2)) && assert(count)(equalTo(2L))
      },
      test("get an transaction by its id") {
        for {
          stmt <- TransactionRepository.getBy(lineTransactionId1.toString,company)
        } yield  assert(stmt.id)(equalTo(lineTransactionId1))
      }
    ).provideCustomLayerShared(testLayer.orDie) @@ sequential
}
