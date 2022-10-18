package com.kabasoft.iws

import com.kabasoft.iws.config.DbConfig
import com.kabasoft.iws.domain.Customer_OLD
import com.kabasoft.iws.repository.{ CustomerOLDRepository, CustomerOLDRepositoryImpl }
import zio.sql.ConnectionPool
import zio._
import java.time.LocalDate
import java.util.UUID

object CustomerImportApp extends ZIOAppDefault {

  val customers = List(
    Customer_OLD(UUID.randomUUID(), "Peter", "Schwarz", false, LocalDate.now()),
    Customer_OLD(UUID.randomUUID(), "Laszlo", "Wider", true, LocalDate.now())
  )

  def run = (for {
    _ <- CustomerOLDRepository.add(customers)
    _ <- ZIO.debug(s"  EXIT")
  } yield ()).provide(DbConfig.layer, ConnectionPool.live, DbConfig.connectionPoolConfig, CustomerOLDRepositoryImpl.live)
}
