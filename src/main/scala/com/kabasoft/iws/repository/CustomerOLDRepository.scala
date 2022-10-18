package com.kabasoft.iws.repository

import zio.stream._
import com.kabasoft.iws.domain._
import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._

import java.util.UUID

trait CustomerOLDRepository {

  def findAll(): ZStream[Any, RepositoryError, Customer_OLD]

  def findById(id: UUID): ZIO[Any, RepositoryError, Customer_OLD]

  def add(customer: Customer_OLD): ZIO[Any, RepositoryError, Unit]

  def add(customer: List[Customer_OLD]): ZIO[Any, RepositoryError, Int]

  def removeAll(): ZIO[Any, RepositoryError, Int]

  def modify(model: Customer_OLD): ZIO[Any, RepositoryError, Int]
}

object CustomerOLDRepository {
  def findAll(): ZStream[CustomerOLDRepository, RepositoryError, Customer_OLD] =
    ZStream.serviceWithStream[CustomerOLDRepository](_.findAll())

  def findById(id: UUID): ZIO[CustomerOLDRepository, RepositoryError, Customer_OLD] =
    ZIO.serviceWithZIO[CustomerOLDRepository](_.findById(id))

  def add(
    customer: Customer_OLD
  ): ZIO[CustomerOLDRepository, RepositoryError, Unit] =
    ZIO.serviceWithZIO[CustomerOLDRepository](_.add(customer))

  def add(
    customer: List[Customer_OLD]
  ): ZIO[CustomerOLDRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[CustomerOLDRepository](_.add(customer))

  def removeAll(): ZIO[CustomerOLDRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[CustomerOLDRepository](_.removeAll())

  def modify(c: Customer_OLD): ZIO[CustomerOLDRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[CustomerOLDRepository](_.modify(c))
}
