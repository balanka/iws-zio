package com.kabasoft.iws

import com.kabasoft.iws.config.DbConfig
import com.kabasoft.iws.domain.Customer
import com.kabasoft.iws.repository.{CustomerRepository, CustomerRepositoryImpl}
import zio.sql.ConnectionPool
import zio._
import java.time.LocalDate
import java.util.UUID

object CustomerImportApp extends ZIOAppDefault {

  val customers = List(
    Customer(UUID.randomUUID(), "Peter", "Schwarz", false, LocalDate.now()),
    Customer(UUID.randomUUID(), "Laszlo", "Wider", true, LocalDate.now())
  )

  def run = (for {
    _ <- CustomerRepository.add(customers)
    _ <- ZIO.debug(s"  EXIT")
  } yield ()).provide(DbConfig.layer, ConnectionPool.live, DbConfig.connectionPoolConfig, CustomerRepositoryImpl.live)
}
