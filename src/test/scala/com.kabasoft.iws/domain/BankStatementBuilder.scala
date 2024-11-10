package com.kabasoft.iws.domain

import java.math.{BigDecimal, RoundingMode}
import java.time.Instant

object BankStatementBuilder {
  val company ="1000"
  val id = "4711"
  val posted = false
  val amount = new BigDecimal("2000.00").setScale(2, RoundingMode.HALF_UP)
  val amount1 = new BigDecimal("-3000.00").setScale(2, RoundingMode.HALF_UP)
  val modelid = BankStatement.MODELID
  val period = common.getPeriod(Instant.now())
  val bs = List(
    BankStatement(3, "B Mady",Instant.now(), Instant.now(),"TEST POSTING","TEST PURPOSE","B Mady"
      ,"DE27662900000001470004X","43007711BIC", amount, "EUR","INFO TXT","1000","4711430IBAN", posted, modelid, period ),
    BankStatement(4, "KABA Soft GmbH",Instant.now(), Instant.now(),"TEST POSTING","TEST PURPOSE","KABA Soft GmbH"
      ,"DE27662900000001470034X","43007711BIC", amount1, "EUR","INFO TXT","1000","470434300IBAN", posted, modelid, period )
  )

}