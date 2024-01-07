package com.kabasoft.iws.domain

import java.math.BigDecimal
import java.time.Instant

object AccountBuilder {
  val companyId ="1000"
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
  val incaccountId10 = "4410"
  val incaccountId1 = "4400"
  val incaccountName1 = "Verbindlichkeit"
  val accountId = "1810"
  val accountName = "Bank"
  val accountLiabId = "3300"
  val accountLiabId1 = "3701"
  val accountLiabId2 = "3310"
  val accountRecvId = "1215"
  val pvataccountId = "3800"
  val vataccountId = "3806"
  val vataccountName = "MWst 19"
  val oaccountId2 = "4714"
  val faccountId = "1200"
  val faccountName = "Forderung"
  val name = "Giro SPK Bielefeld"
  val fname = "Forderungen aus Lieferungen und Leistungen"
  val modelid = 112
  val period = common.getPeriod(Instant.now())
  val side = true
  val terms = "terms"
  val ccy = "EUR"
  val t = Instant.now()
  val zero = BigDecimal.valueOf(0, 2 )
  val vtime = Instant.parse("2018-01-01T10:00:00.00Z")
  val INST_ACC =  Account(paccountId0, "Bilanz","Bilanz",t , t, t, "1000", Account.MODELID, "", true, true, ccy, zero, zero, zero, zero, Nil.toSet)
  val INST_ACC1 =  Account(paccountId1, "Bilanz Aktiva","Bilanz Aktiva",t , t, t, companyId, Account.MODELID, paccountId0, true, true, ccy, zero, zero, zero, zero, Nil.toSet)
  val INST_ACC2 =  Account(paccountId2, "Bilanz Passiva","Bilanz Passiva",t , t, t, companyId, Account.MODELID, paccountId0, false, true, ccy, zero, zero, zero, zero, Nil.toSet)
  val REV_ACC8 =  Account(raccountId8, "Jahresueberschuss","Jahresueberschuss", t, t, t, companyId, Account.MODELID, paccountId0, false, true, "EUR", zero,zero,zero,zero, Nil.toSet)
  val REV_ACC =  Account(raccountId, "G.u.V","G.u.V", t, t, t, companyId, Account.MODELID, paccountId0, false, true, "EUR", zero,zero,zero,zero, Nil.toSet)
  val REV_ACC1 =  Account(raccountId1, "G.u.V Aktiva","G.u.V Aktiva", t, t, t, companyId, Account.MODELID, paccountId0, false, true, "EUR", zero,zero,zero,zero, Nil.toSet)
  val REV_ACC2 =  Account(raccountId2, "G.u.V Passiva","G.u.V Passiva", t, t, t, companyId, Account.MODELID, paccountId0, false, true, "EUR", zero,zero,zero,zero, Nil.toSet)
  val faccount =  Account(faccountId, fname,fname, t, t, t, companyId, Account.MODELID, paccountId1, true, true, ccy, zero,zero,zero,zero, Nil.toSet)
  val baccount =  Account(bankaccountId, "Bank","Bank", t, t, t, companyId,
     Account.MODELID, paccountId1, true, true, ccy, zero,zero,zero,zero, Nil.toSet)
  val baccount1 =  Account(accountId, name,"Giro SPK Bielefeld", t, t, t, companyId,
    Account.MODELID, bankaccountId, true, true, ccy, zero,zero,zero,zero, Nil.toSet)
  val receivaccount =
    Account(accountRecvId, "Forderung", "Forderung", t, t, t, companyId, Account.MODELID, raccountId1, true, true, ccy, zero, zero, zero, zero, Nil.toSet)

  val liabilityAccount =
    Account(accountLiabId, "Verbindlichkeiten", "Verbindlichkeiten", t, t, t, companyId, Account.MODELID, paccountId2, true, true, ccy, zero, zero, zero, zero, Nil.toSet)

  val liabilityAccount1 =
    Account(accountLiabId1, "Verbindlichkeiten aus Steuer und Abgaben ", "Verbindlichkeiten aus Steuer und Abgaben", t, t, t, companyId, Account.MODELID, accountLiabId, true, true, ccy, zero, zero, zero, zero, Nil.toSet)

  val vataccount =  Account(vataccountId, "Umsatzsteuer 19 %","Umsatzsteuer 19 %",t,t,t, companyId,
     Account.MODELID, accountLiabId1, false, true, "EUR", zero, zero, zero, zero, Nil.toSet)
  val raccount =  Account(incaccountId, "Umsatzerloese","Umsatzerloese",t,t,t, companyId,
    Account.MODELID, raccountId2, false, true, "EUR", zero, zero, zero, zero, Nil.toSet)
  val raccount1 =  Account(incaccountId1, "Umsatzerloese 19%","Umsatzerloese 19%",t,t,t, companyId,
    Account.MODELID, incaccountId, false, true, "EUR", zero, zero, zero, zero, Nil.toSet)

  val acc = Account("00000", "Dummy", "Dummy", vtime, vtime, vtime, companyId, Account.MODELID, "5", true, true, "EUR", zero, zero, zero, zero, Nil.toSet)
  val accx = Account("000001", "Dummy", "Dummy", vtime, vtime, vtime, companyId, Account.MODELID, "5", true, true, "EUR", zero, zero, zero, zero, Nil.toSet)


  val list =List(INST_ACC, INST_ACC1, INST_ACC2, REV_ACC8, REV_ACC, REV_ACC1, REV_ACC2, faccount
    , baccount, baccount1,raccount,raccount1, vataccount, liabilityAccount, liabilityAccount1)

}