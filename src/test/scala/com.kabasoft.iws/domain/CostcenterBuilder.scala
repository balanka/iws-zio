package com.kabasoft.iws.domain

import java.time.Instant

object CostcenterBuilder {
  val companyId ="1000"
  val id ="300"
  val idx ="300x"
  val accId ="800"
  val ccName ="Production"
  val vtime = Instant.parse("2018-01-01T10:00:00.00Z")
  val cc = Costcenter(id, ccName, ccName, accId, vtime, vtime, vtime, 6, companyId)
  val ccx = Costcenter(idx, ccName, ccName, accId, vtime, vtime, vtime, 6, companyId)
}