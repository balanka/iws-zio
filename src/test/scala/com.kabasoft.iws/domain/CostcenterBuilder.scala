package com.kabasoft.iws.domain

import java.time.Instant

object CostcenterBuilder {
  val companyId ="1000"
  val ccId ="300"
  val accId ="800"
  val ccName ="Production"
  val vtime = instantFromStr("2018-01-01T00:00:00.00Z")
  val cc = Costcenter(ccId, ccName, ccName, accId, vtime, vtime, vtime, 6, companyId)

  def instantFromStr(str:String)=Instant.parse(str)
}