package com.kabasoft.iws.domain

import com.kabasoft.iws.domain.AccountBuilder.{accountRecvId, companyId, incaccountId, taxCode, vataccountId}

import java.time.Instant

object SupplierBuilder {
  val supplierId1 = "4711000"
  val supplierId2 = "4712000"
  val supId = "700000x"
  val supIdx = "70001x"
  val vat = "v5"
  val supName= "Dummy"
  val accId = "331040"
  val currency = "EUR"                      
  val bankAccountId0 = "DE844805016104782700XXX"
  val bankAccountId =  "DE855805016104782700XXX"
  val name = "Sontige Lieferant Rechts und Beratung"
  val vtime = Instant.parse("2018-01-01T10:00:00.00Z")
  val bankAccount0 = BankAccount(bankAccountId0, "SPBIDE3XXX", supplierId1, companyId, BankAccount.MODEL_ID)
  val bankAccount = BankAccount(bankAccountId, "SPBIDE3XXX", supplierId1, companyId, BankAccount.MODEL_ID)
  val bankAccounts = List(bankAccount0, bankAccount)
  val supplier1 = Supplier(supplierId1, "B Mady", "Bintou Mady Kaba","Bielefelder Str 1", "33615" , "Bielefeld", "NRW", "DE", "0521-471163"
      ,"xxx@domain.de", accountRecvId, incaccountId, taxCode, vataccountId, currency, companyId, Supplier.MODELID, vtime, vtime, vtime, bankAccounts)

  val supplier2 = Supplier(supplierId2, "KABA Soft GmbH", "KABA Soft GmbH", "Universitaet Str 2", "33615", "Bielefeld", "NRW","DE",
      "0521-4711631", "info@kabasoft.de", accountRecvId, incaccountId, taxCode, vataccountId, currency, companyId, Supplier.MODELID, vtime, vtime,vtime)

  val sup = Supplier(supId, supName, supName, "", "", "", "", "DE","", "", accId, "6825",  taxCode, vat, currency, companyId, Supplier.MODELID, vtime, vtime,vtime)
  val supx = Supplier(supIdx, supName, supName, "", "", "", "", "DE","", "", accId, "6825", taxCode,  vat,currency, companyId, Supplier.MODELID, vtime, vtime,vtime)
  val suppliers = List( supplier1, supplier2 )

}