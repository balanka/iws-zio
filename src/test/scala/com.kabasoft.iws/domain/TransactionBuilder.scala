package com.kabasoft.iws.domain

import com.kabasoft.iws.domain.AccountBuilder.{accountId, faccountId, incaccountId1, vataccountId}

import java.math.{BigDecimal, RoundingMode}
import java.time.Instant

object TransactionBuilder {
  val company ="1000"
  val transactionId = -1L// 4711L
  val transactionId2 = -2L// 4711L
  val lineTransactionId1 = 1L
  val lineTransactionId2 = 2L
  val modelid = 112
  val modelid2 = 114
  val period = common.getPeriod(Instant.now())
  val side = true
  val amount = new BigDecimal("100.00").setScale(2, RoundingMode.HALF_UP)
  val vat = new BigDecimal("0.19").setScale(2, RoundingMode.HALF_UP)
  val vatAmount = amount.multiply(vat)
  val terms = "terms"
  val currency = "EUR"
  val costCenter = "311"

  val line1=  FinancialsTransactionDetails(-1, 0, faccountId, side, incaccountId1 , amount.subtract(vatAmount), Instant.now(), terms, currency)
  val line2=  FinancialsTransactionDetails(-1, 0, faccountId, side, vataccountId, vatAmount, Instant.now(), terms, currency)
  val line3 = FinancialsTransactionDetails(-3, 0, accountId, side, faccountId, amount.add(vatAmount) , Instant.now(), terms, currency)
  val line4 = FinancialsTransactionDetails(-4, 0, accountId, side, faccountId, amount.add(vatAmount) , Instant.now(), terms, currency)
  val line5 = FinancialsTransactionDetails(1, 0, faccountId, side, incaccountId1, amount.subtract(vatAmount) , Instant.now(), terms, currency)
  val line6 = FinancialsTransactionDetails(1, 0, faccountId, side, vataccountId, vatAmount , Instant.now(), terms, currency)




  val ftr1 = FinancialsTransaction(0,-1,costCenter, accountId, Instant.now(), Instant.now(), Instant.now()
    , period, false, modelid, company, "comments"+modelid, -1,-1, List(line1, line2))
  val ftr2 = FinancialsTransaction(0, -1, costCenter, accountId, Instant.now(), Instant.now(), Instant.now()
    , period, false, modelid2, company, "comments", -1, -1, List(line3))
  val ftr4 = FinancialsTransaction(0, -1, costCenter, accountId, Instant.now(), Instant.now(), Instant.now()
    , period, false, modelid2, company, "comments", -1, -1, List(line4))
  val ftr5 = FinancialsTransaction(1, -1, costCenter, accountId, Instant.now(), Instant.now(), Instant.now()
    , period, false, modelid2, company, "comments", -1, -1, List(line5, line6))
  val dtransactions = ftr1.toDerive()
  val pacs = PeriodicAccountBalance.create(ftr1).distinct



}