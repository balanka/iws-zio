package com.kabasoft.iws.domain

import com.kabasoft.iws.domain.AccountBuilder._
import com.kabasoft.iws.domain.ArticleBuilder.{artId0, artId1}

import java.math.{BigDecimal, RoundingMode}
import java.time.Instant

object TransactionBuilder {
  val company ="1000"
  val lineTransactionId1 = 1L
  val lineTransactionId2 = 2L

  val modelid = TransactionModelId.GOORECEIVING.id
  val modelid2 = TransactionModelId.SUPPLIER_INVOICE.id
  val vtime = Instant.now()
  val period = common.getPeriod(vtime)
  val side = true
  val quantity0 = new BigDecimal("100.00").setScale(2, RoundingMode.HALF_UP)
  val quantity1 = new BigDecimal("100.00").setScale(2, RoundingMode.HALF_UP)
  val pprice0 = new BigDecimal("10.00").setScale(2, RoundingMode.HALF_UP)
  val pprice1 = new BigDecimal("50.00").setScale(2, RoundingMode.HALF_UP)
  val terms = "Purchasing of good  or service"
  val currency = "EUR"
  val store = "311"
  val qttyUnit = "stk"

  val line1=  TransactionDetails(-1L, 0L,  artId0 , quantity0, qttyUnit, pprice0, currency, vtime, terms )
  val line2=  TransactionDetails(-1L, 0L,  artId1 , quantity1, qttyUnit, pprice1, currency, vtime, terms )

  val ftr1 = Transaction(0,-1, -1,  store, accountId, vtime, vtime, vtime, period, posted = false, modelid, company, terms
          ,  List(line1, line2))
  val ftr2 = ftr1.copy(modelid = modelid2, lines = ftr1.lines.map(l=>TransactionDetails(l)))

}