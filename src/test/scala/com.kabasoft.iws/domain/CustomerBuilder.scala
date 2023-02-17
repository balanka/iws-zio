package com.kabasoft.iws.domain
import java.time.Instant
import com.kabasoft.iws.domain.Customer
import com.kabasoft.iws.domain.AccountBuilder.{accountRecvId, incaccountId, vataccountId}

object CustomerBuilder {
  val customerId1 = "4711"
  val customerId2 = "4712"
  val customerId3 = "5004"
  val dummyId     = "5222"
  val nameCustomer1 = "B Mady"
  val nameCustomer2 = "KABA Soft GmbH"
  val IbanCustomer1 ="DE27662900000001470034X"
  val IbanCustomer2 ="DE22480501610043000000"
  val IbanCustomer3 ="DE27662900000001470004X"
  val nameCustomer3 = "Kunde ( Sonstige Erloes)"
  val dummyNname   = "Dummy"
   val vtime = instantFromStr("2018-01-01T00:00:00.00Z")
  val customer1 = Customer(customerId1, nameCustomer1, "Bintou Mady Kaba","Bielefelder Str 1", "33615" , "Bielefeld", "NRW", "DE",  "0521-471163",
      "0521-471164","xxx@domain.de", accountRecvId, incaccountId, IbanCustomer1, vataccountId,1000)
  val customer2 = Customer(customerId2, nameCustomer2, "KABA Soft GmbH", "Universitaet Str 2", "33615", "Bielefeld", "NRW", "DE",
      "0521-4711631", "0521-4711641", "info@fabasoft.de", accountRecvId, incaccountId, IbanCustomer2, vataccountId, 1000)

  val dummy = Customer(dummyId, dummyNname, dummyNname, dummyNname, dummyNname, dummyNname, dummyNname, dummyNname,"DE",
       "dummy@dummy.com", "1215", "111111", "DEddddddddddddddddommy", "v5", "1000", Customer.MODELID, vtime, vtime, vtime)
  val customers = List( customer1, customer2 )

    def instantFromStr(str:String)=Instant.parse(str)

}