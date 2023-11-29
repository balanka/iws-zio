package com.kabasoft.iws.domain

import java.time.Instant

object BankBuilder {
  val company = "1000"
  val bankId = "COLSDE33"
  val bankIdx = "COLSDE33X"
  val bankName = "SPARKASSE KOELN-BONN"

  val vtime = Instant.parse("2018-01-01T10:00:00.00Z")
  val bank = Bank(bankId, bankName, bankName, vtime, vtime,vtime, 11, company)
  val bankx = Bank(bankIdx, bankName, bankName, vtime, vtime,vtime, 11, company)

}