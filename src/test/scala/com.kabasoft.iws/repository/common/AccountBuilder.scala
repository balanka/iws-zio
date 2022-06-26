package com.kabasoft.iws.repository.common

import com.kabasoft.iws.domain.{Account, common}
import java.time.Instant

object AccountBuilder {
  val company ="1000"
  //val paccountId = "9999"
  val paccountId0 = "9900"
  val paccountId1 = "9901"
  val paccountId2 = "9902"
  val raccountId = "9800"
  val raccountId1 = "9801"
  val raccountId2 = "9802"
  val raccountId8 = "9808"
  val bankaccountId = "1800"
  val incaccountId = "4000"
  val incaccountId1 = "4400"
  val accountId = "1810"
  val pvataccountId = "3800"
  val vataccountId = "3806"
  val oaccountId2 = "4714"
  val faccountId = "1200"
  val name = "Giro SPK Bielefeld"
  val fname = "Forderungen aus Lieferungen und Leistungen"
  val modelid = 112
  val period = common.getPeriod(Instant.now())
  val side = true
  val terms = "terms"
  val ccy = "EUR"
  val t = Instant.now()
  val zero = BigDecimal(0)
  val INST_ACC =  Account(paccountId0, "Bilanz","Bilanz",t , t, t, "1000", Account.MODELID, "", true, true, ccy, zero, zero, zero, zero, Nil.toSet)
  val INST_ACC1 =  Account(paccountId1, "Bilanz Aktiva","Bilanz Aktiva",t , t, t, company, Account.MODELID, paccountId0, true, true, ccy, zero, zero, zero, zero, Nil.toSet)
  val INST_ACC2 =  Account(paccountId2, "Bilanz Passiva","Bilanz Passiva",t , t, t, company, Account.MODELID, paccountId0, false, true, ccy, zero, zero, zero, zero, Nil.toSet)
  val REV_ACC8 =  Account(raccountId8, "Jahresueberschuss","Jahresueberschuss", t, t, t, company, Account.MODELID, paccountId0, false, true, "EUR", zero,zero,zero,zero, Nil.toSet)
  val REV_ACC =  Account(raccountId, "G.u.V","G.u.V", t, t, t, company, Account.MODELID, paccountId0, false, true, "EUR", zero,zero,zero,zero, Nil.toSet)
  val REV_ACC1 =  Account(raccountId1, "G.u.V Aktiva","G.u.V Aktiva", t, t, t, company, Account.MODELID, paccountId0, false, true, "EUR", zero,zero,zero,zero, Nil.toSet)
  val REV_ACC2 =  Account(raccountId1, "G.u.V Passiva","G.u.V Passiva", t, t, t, company, Account.MODELID, paccountId0, false, true, "EUR", zero,zero,zero,zero, Nil.toSet)
  val faccount =  Account(faccountId, fname,fname, t, t, t, company, Account.MODELID, paccountId1, true, true, ccy, zero,zero,zero,zero, Nil.toSet)
  val baccount =  Account(bankaccountId, "Bank","Bank", t, t, t, company,
     Account.MODELID, raccountId1, true, true, ccy, zero,zero,zero,zero, Nil.toSet)
  val baccount1 =  Account(accountId, name,"Giro SPK Bielefeld", t, t, t, company,
    Account.MODELID, bankaccountId, true, true, ccy, zero,zero,zero,zero, Nil.toSet)
  val vataccount =  Account(vataccountId, "Umsatzsteuer 19 %","Umsatzsteuer 19 %",t,t,t, company,
     Account.MODELID, pvataccountId, false, true, "EUR", zero, zero, zero, zero, Nil.toSet)
  val raccount =  Account(incaccountId, "Umsatzerloese","Umsatzerloese",t,t,t, company,
    Account.MODELID, raccountId2, false, true, "EUR", zero, zero, zero, zero, Nil.toSet)
  val raccount1 =  Account(incaccountId1, "Umsatzerloese 19%","Umsatzerloese 19%",t,t,t, company,
    Account.MODELID, incaccountId, false, true, "EUR", zero, zero, zero, zero, Nil.toSet)

  val list =List(INST_ACC, INST_ACC1, INST_ACC2, REV_ACC8, REV_ACC, REV_ACC1, REV_ACC2, faccount
    , baccount, baccount1,raccount,raccount1, vataccount)


}