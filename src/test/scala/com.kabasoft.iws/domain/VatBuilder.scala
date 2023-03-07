package com.kabasoft.iws.domain

import com.kabasoft.iws.domain.AccountBuilder.companyId
import java.math.{BigDecimal, RoundingMode}
import java.time.Instant

object VatBuilder {
  val vatId1 = "v101"
  val vatId1x = "v101x"
  val name = "Dummy"
  val vtime = instantFromStr("2018-01-01T10:00:00.00Z")
  val amount =  new BigDecimal("0.07").setScale(2, RoundingMode.UNNECESSARY)
  val vat1 = Vat(vatId1, name, name, amount, "0650" , "0651", vtime,  vtime, vtime, companyId, 14)
  val vat1x = Vat(vatId1x, name, name, amount, "0650" , "0651", vtime,  vtime, vtime, companyId, 14)
  def instantFromStr(str:String)=Instant.parse(str)

}