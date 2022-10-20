package com.kabasoft.iws.domain

import com.kabasoft.iws.domain. BankStatement
import java.time.Instant

object BankStatementBuilder {
  val company ="1000"
  val id = "4711"

  val bs = List(
    BankStatement(4711, "B Mady",Instant.now(), Instant.now(),"TEST POSTING","TEST PURPOSE","B Mady"
      ,"DE27662900000001470034X","43007711BIC", -2000, "EUR","INFO TXT","1000","4711430IBAN" ),
    BankStatement(4712, "KABA Soft GmbH",Instant.now(), Instant.now(),"TEST POSTING","TEST PURPOSE","KABA Soft GmbH"
      ,"DE27662900000001470004X","43007711BIC", 3000, "EUR","INFO TXT","1000","470434300IBAN" )
  )

}