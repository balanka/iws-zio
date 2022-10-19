package com.kabasoft.iws.domain

import com.kabasoft.iws.domain.Customer
import AccountBuilder.{accountRecvId, incaccountId, vataccountId}

object CustomerBuilder {
  val customerId1 = "4711"
  val customerId2 = "4712"
  val nameCustomer1 = "B Mady"
  val nameCustomer2 = "KABA Soft GmbH"
  val customer1 = Customer(customerId1, nameCustomer1, "Bintou Mady Kaba","Bielefelder Str 1", "33615" , "Bielefeld", "NRW", "DE", "0521-471163",
      "0521-471164","xxx@domain.de", accountRecvId, incaccountId, "DE22480501610043719244_X", vataccountId,1000)
  val customer2 = Customer(customerId2, nameCustomer2, "KABA Soft GmbH", "Universitaet Str 2", "33615", "Bielefeld", "NRW", "DE",
      "0521-4711631", "0521-4711641", "info@kabasoft.de", accountRecvId, incaccountId, "DE22480501610043719244_Y", vataccountId, 1000)
  val customers = List( customer1, customer2 )

}