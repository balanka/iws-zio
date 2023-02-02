package com.kabasoft.iws.domain

import com.kabasoft.iws.domain.AccountBuilder.{accountRecvId, incaccountId, vataccountId}

object CustomerBuilder {
  val customerId1 = "4711"
  val customerId2 = "4712"
  val customerId3 = "5004"
  val nameCustomer1 = "B Mady"
  val nameCustomer2 = "KABA Soft GmbH"
  val IbanCustomer1 ="DE27662900000001470034X"
  val IbanCustomer2 ="DE22480501610043000000"
  val IbanCustomer3 ="DE27662900000001470004X"
  val nameCustomer3 = "Kunde ( Sonstige Erloes)"
  val customer1 = Customer(customerId1, nameCustomer1, "Bintou Mady Kaba","Bielefelder Str 1", "33615" , "Bielefeld", "NRW",  "0521-471163",
      "0521-471164","xxx@domain.de", accountRecvId, incaccountId, IbanCustomer1, vataccountId,1000)
  val customer2 = Customer(customerId2, nameCustomer2, "KABA Soft GmbH", "Universitaet Str 2", "33615", "Bielefeld", "NRW",
      "0521-4711631", "0521-4711641", "info@kabasoft.de", accountRecvId, incaccountId, IbanCustomer2, vataccountId, 1000)
  val customers = List( customer1, customer2 )

}