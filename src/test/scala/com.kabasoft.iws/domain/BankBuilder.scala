package com.kabasoft.iws.domain

import java.time.Instant

object BankBuilder {
  val company = "1000"
  val bankId = "COLSDE33"
  val bankName = "SPARKASSE KOELN-BONN"
  val accId = "9900"
  val accName = "Bilanz"
  val vtime = instantFromStr("2018-01-01T00:00:00.00Z")
  val bank = Bank(bankId, bankName, bankName, vtime, vtime,vtime, 11, company)

  def instantFromStr(str:String)=Instant.parse(str)


}