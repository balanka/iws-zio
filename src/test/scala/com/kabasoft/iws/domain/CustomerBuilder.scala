package com.kabasoft.iws.domain
import java.time.Instant
import com.kabasoft.iws.domain.Customer
import com.kabasoft.iws.domain.AccountBuilder.{accountRecvId, incaccountId, taxCode, vataccountId}

object CustomerBuilder:
  val customerId1 = "571100x"
  val customerId2 = "571200x"
  val customerId3 = "5004v"
  val dummyId     = "5222v"
  val dummyIdx     = "5222x"
  val companyId    = "-1000"
  val nameCustomer1 = "B Mady"
  val nameCustomer2 = "KABA Soft GmbH"
  val bankAccountId0 ="DE27662900000001470034X"
  val bankAccountId ="DE22480501610043000000X"
  val IbanCustomer3 ="DE27662900000001470004X"
  val nameCustomer3 = "Kunde ( Sonstige Erloes)"
  val dummyNname   = "Dummy"
  val currency ="EUR"
   val vtime = Instant.parse("2018-01-01T10:00:00.00Z")
  // val bankAccountId0 = "4711XXXX"
  //val bankAccountId = "4712XXXX"
   //                           id: String, name: String, description: String, street: String, zip: String, city: String, state: String, country: String, phone: String, email: String,
  //                           account: String,
  //                           oaccount: String,
  //                           taxCode: String,
  //                           vatCode: String,
  //                           currency: String,
  //                           company: String,
  //                           modelid: Int = Customer.MODELID,
  val bankAccount0 = BankAccount(bankAccountId0, "SPBIDE3XXX", customerId1, companyId, BankAccount.MODEL_ID)
  val bankAccount = BankAccount(bankAccountId, "SPBIDE3XXX", customerId1, companyId, BankAccount.MODEL_ID)
   val bankAccounts =  List(bankAccount0, bankAccount)
  val customer1 = Customer(customerId1, nameCustomer1, "Bintou Mady Kaba", "Bielefelder Str 1", "33615" , "Bielefeld", "NRW", "DE",  "0521-471163"
     ,"xxx@domain.de", accountRecvId, incaccountId, taxCode, vataccountId, currency, companyId, Customer.MODELID, vtime, vtime, vtime, bankAccounts)
  val customer2 = Customer(customerId2, nameCustomer2, "KABA Soft GmbH", "Universitaet Str 2", "33615", "Bielefeld", "NRW", "DE",
      "0521-4711631", "info@fabasoft.de", accountRecvId, incaccountId, taxCode, vataccountId, currency, companyId, Customer.MODELID, vtime, vtime, vtime)

  val cust = Customer(dummyId, dummyNname, dummyNname, dummyNname, dummyNname, dummyNname, dummyNname, "DE", dummyNname,
       "dummy@dummy.com", "1215", "111111",  taxCode, "v5", currency, companyId, Customer.MODELID, vtime, vtime, vtime)
  val custx = Customer(dummyIdx, dummyNname, dummyNname, dummyNname, dummyNname, dummyNname, dummyNname, "DE", dummyNname,
    "dummy@dummy.com", "1215", "111111", taxCode,  "v5", currency, companyId, Customer.MODELID, vtime, vtime, vtime)
  val customers = List( customer1, customer2 )