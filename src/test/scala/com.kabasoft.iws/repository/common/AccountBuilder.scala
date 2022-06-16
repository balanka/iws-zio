package com.kabasoft.iws.repository.common

import com.kabasoft.iws.domain.{Account, common}
import java.time.Instant

object AccountBuilder {
  val company ="1000"
  val paccountId = "9999"
  val accountId = "4712"
  val oaccountId = "4713"
  val oaccountId2 = "4714"
  val name = "MyAccount"
  val modelid = 112
  val period = common.getPeriod(Instant.now())
  val side = true
  val terms = "terms"
  val currency = "EUR"

  val paccount =  Account(paccountId, "ParentAccountName","ParentAccountDescription", Instant.now(), Instant.now(), Instant.now()
    , "1000", Account.MODELID, "", true, true, "EUR", BigDecimal(0), BigDecimal(0), BigDecimal(0), BigDecimal(0), Nil.toSet)
  val account =  Account(accountId, name,"MyAccount", Instant.now(), Instant.now(), Instant.now()
    , "1000", Account.MODELID, paccountId, true, true, "EUR", BigDecimal(0), BigDecimal(0), BigDecimal(0), BigDecimal(0), Nil.toSet)
  val oaccount1 =  Account(oaccountId, "MyAccount2","MyAccount2", Instant.now(), Instant.now(), Instant.now()
    , "1000", Account.MODELID, paccountId, false, true, "EUR", BigDecimal(0), BigDecimal(0), BigDecimal(0), BigDecimal(0), Nil.toSet)
  val oaccount2 =  Account(oaccountId2, "MyAccount2","MyAccount2", Instant.now(), Instant.now(), Instant.now()
    , "1000", Account.MODELID, paccountId, false, true, "EUR", BigDecimal(0), BigDecimal(0), BigDecimal(0), BigDecimal(0), Nil.toSet)

  val list =List(paccount, account, oaccount1, oaccount2)


}