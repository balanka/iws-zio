package com.kabasoft.iws

import com.kabasoft.iws.service.Transfer
import zio._

object MySTMApp extends ZIOAppDefault {

  def run = Transfer.main

}
