package com.kabasoft.iws.domain
import AccountBuilder.{accountId, faccountId, vataccountId, incaccountId1}

import java.time.Instant

object TransactionBuilder {
  val company ="1000"
  val transactionId = -1L// 4711L
  val transactionId2 = -2L// 4711L
  val lineTransactionId1 = 1L
  val lineTransactionId2 = 2L
  val modelid = 112
  val period = common.getPeriod(Instant.now())
  val side = true
  val amount = BigDecimal(100)
  val vat = 0.19
  val vatAmount = amount*vat
  val terms = "terms"
  val currency = "EUR"
  val costCenter = "311"

  val line1=  FinancialsTransactionDetails(-1, transactionId, faccountId, side, incaccountId1 , amount-vatAmount, Instant.now(), terms, currency)
  val line2=  FinancialsTransactionDetails(-2, transactionId, faccountId, side, vataccountId, vatAmount, Instant.now(), terms, currency)
  val line3 = FinancialsTransactionDetails(-3, transactionId2, accountId, side, faccountId, amount+vatAmount , Instant.now(), terms, currency)



  val ftr1 = FinancialsTransaction(transactionId,-1,costCenter, accountId, Instant.now(), Instant.now(), Instant.now()
    , period, false, modelid, company, "comments", -1,-1, List(line1, line2))
  val ftr2 = FinancialsTransaction(transactionId2, -1, costCenter, accountId, Instant.now(), Instant.now(), Instant.now()
    , period, false, modelid, company, "comments", -1, -1, List(line3))
  val dtransactions = ftr1.toDerive()
  val pacs = PeriodicAccountBalance.create(ftr1).distinct



}