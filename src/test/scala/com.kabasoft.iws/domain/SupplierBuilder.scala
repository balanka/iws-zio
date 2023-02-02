package com.kabasoft.iws.domain

import com.kabasoft.iws.domain.AccountBuilder.{accountRecvId, incaccountId, vataccountId}

object SupplierBuilder {
  val supplierId1 = "4711"
  val supplierId2 = "4712"
  val supplierIban3 = "DE27662900000001470034X"
  val supplier1 = Supplier(supplierId1, "B Mady", "Bintou Mady Kaba","Bielefelder Str 1", "33615" , "Bielefeld", "NRW",  "0521-471163",
      "0521-471164","xxx@domain.de", accountRecvId, incaccountId, supplierIban3, vataccountId,1000)
  val supplier2 = Supplier(supplierId2, "KABA Soft GmbH", "KABA Soft GmbH", "Universitaet Str 2", "33615", "Bielefeld", "NRW",
      "0521-4711631", "0521-4711641", "info@kabasoft.de", accountRecvId, incaccountId, "DE22480501610043719244_Y", vataccountId, 1000)
  val suppliers = List( supplier1, supplier2 )

}