package com.kabasoft.iws.domain

import java.time.Instant

object ModuleBuilder {
  val companyId ="1000"
  val id ="0000"
  val idx ="000x"
  val name ="Dummy"
  val path =""
  val vtime = instantFromStr("2018-01-01T00:00:00.00Z")
  val m = Module(id, name, name, path, vtime,vtime,vtime, 300, companyId)
  val mx = Module(idx, name, name, path, vtime,vtime,vtime, 300, companyId)

  def instantFromStr(str:String)=Instant.parse(str)
}