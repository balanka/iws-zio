package com.kabasoft.iws.repository.common

import com.kabasoft.iws.domain.{FinancialsTransaction, FinancialsTransactionDetails, PeriodicAccountBalance, common}
import AccountBuilder.{accountId, oaccountId, oaccountId2}

import java.time.Instant

object TransactionBuilder {
  val company ="1000"
  val transactionId = 4711L
  val lineTransactionId1 = 1L
  val lineTransactionId2 = 2L
  val modelid = 112
  val period = common.getPeriod(Instant.now())
  val side = true
  val amount = BigDecimal(100)
  val vat = 0.19
  val vatAmount = BigDecimal(100)*vat
  val terms = "terms"
  val currency = "EUR"

  val line1=  FinancialsTransactionDetails(lineTransactionId1, transactionId, accountId, side, oaccountId, amount-vatAmount, Instant.now(), terms, currency)
  val line2=  FinancialsTransactionDetails(lineTransactionId2, transactionId, accountId, side, oaccountId2, vatAmount, Instant.now(), terms, currency)

  val ftr1 = FinancialsTransaction(transactionId,-1,"311",accountId, Instant.now(), Instant.now(), Instant.now()
    , period, false, modelid, company, "comments", -1,-1, List(line1, line2))
  val dtransactions = ftr1.toDerive()
  val pacs = PeriodicAccountBalance.create(ftr1).distinct



}