package com.kabasoft.iws.domain

import com.kabasoft.iws.domain.AccountBuilder.{accountRecvId, companyId, incaccountId, vataccountId}

import java.time.Instant

object SupplierBuilder {
  val supplierId1 = "4711"
  val supplierId2 = "4712"
  val supId = "70000"
  val supIdx = "70000x"
  val vat = "v5"
  val supName= "Dummy"
  val accId = "331040"
  val supplierIban3 = "DE84480501610047008271"
  val iban = "DE8448050161004700827X"
  val name = "Sontige Lieferant Rechts und Beratung"
  val vtime = instantFromStr("2018-01-01T10:00:00.00Z")
  val supplier1 = Supplier(supplierId1, "B Mady", "Bintou Mady Kaba","Bielefelder Str 1", "33615" , "Bielefeld", "NRW", "DE", "0521-471163",
      "0521-471164","xxx@domain.de", accountRecvId, incaccountId, supplierIban3, vataccountId,1000)

  val supplier2 = Supplier(supplierId2, "KABA Soft GmbH", "KABA Soft GmbH", "Universitaet Str 2", "33615", "Bielefeld", "NRW","DE",
      "0521-4711631", "0521-4711641", "info@kabasoft.de", accountRecvId, incaccountId, "DE22480501610043719244_Y", vataccountId, 1000)

  val sup = Supplier(supId, supName, supName, "", "", "", "", "DE","", "", accId, "6825", iban, vat,companyId, Supplier.MODELID, vtime, vtime,vtime)
  val supx = Supplier(supIdx, supName, supName, "", "", "", "", "DE","", "", accId, "6825", iban, vat,companyId, Supplier.MODELID, vtime, vtime,vtime)
  val suppliers = List( supplier1, supplier2 )

   def instantFromStr(str:String)=Instant.parse(str)

}