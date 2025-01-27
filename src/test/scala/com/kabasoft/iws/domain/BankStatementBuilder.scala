package com.kabasoft.iws.domain

import com.kabasoft.iws.domain.SupplierBuilder//.{bankAccountId0, bankAccountId}
import com.kabasoft.iws.domain.CustomerBuilder //.{bankAccountId0, bankAccountId}
import java.math.{BigDecimal, RoundingMode}
import java.time.Instant

object BankStatementBuilder:
  val company ="-1000"
  val id = "4711"
  val iban1 = "4811XXXX"
  val iban2 = "4711XXXX"
  val posted = false
  val amount = new BigDecimal("2000.00").setScale(2, RoundingMode.HALF_UP)
  val amount1 = new BigDecimal("-3000.00").setScale(2, RoundingMode.HALF_UP)
  val modelid = BankStatement.MODELID
  val period = common.getPeriod(Instant.now())

  val bs1 = BankStatement(3, "B Mady",Instant.now(), Instant.now(),"TEST POSTING","TEST PURPOSE","B Mady"
      , CustomerBuilder.bankAccountId0,"43007711BIC", amount, "EUR","INFO TXT",company,"4711430IBAN", posted, modelid, period )
  val bs2 = BankStatement(4, "KABA Soft GmbH",Instant.now(), Instant.now(),"TEST POSTING","TEST PURPOSE","KABA Soft GmbH"
      , SupplierBuilder.bankAccountId,"43007711BIC", amount1, "EUR","INFO TXT",company,"470434300IBAN", posted, modelid, period )
  val bs = List(bs1, bs2)

