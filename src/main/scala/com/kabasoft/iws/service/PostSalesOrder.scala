package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain._
import zio._

trait  PostSalesOrder:
  def postAll(transactions: List[Transaction], company:Company): ZIO[Any, RepositoryError, Int]

object PostSalesOrder:
  def postAll(transactions: List[Transaction], company:Company): ZIO[PostSalesOrder, RepositoryError, Int] =
    ZIO.serviceWithZIO[PostSalesOrder](_.postAll(transactions, company))

