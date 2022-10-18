package com.kabasoft.iws.domain

import com.kabasoft.iws.domain. BankStatement
import java.time.Instant

object BankStatementBuilder {
  val company ="1000"
  val id = "4711"

  val bs = List(
    BankStatement(4711, "B Mady",Instant.now(), Instant.now(),"TEST POSTING","TEST PURPOSE","B Mady"
      ,"430071244ACTNO","43007711BIC", 1000, "EUR","INFO TXT","1000","4711430IBAN",false,18 ),
    BankStatement(4712, "KABA Soft GmbH",Instant.now(), Instant.now(),"TEST POSTING","TEST PURPOSE","KABA Soft GmbH"
      ,"430000000ACTNO","43007711BIC", 1000, "EUR","INFO TXT","1000","470434300IBAN",false,18 )
  )

}