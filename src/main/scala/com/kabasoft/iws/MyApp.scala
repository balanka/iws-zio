package com.kabasoft.iws

import zio._
import com.kabasoft.iws.service.PacSTMService

object MyApp extends ZIOAppDefault {

  def run = PacSTMService.process

}
