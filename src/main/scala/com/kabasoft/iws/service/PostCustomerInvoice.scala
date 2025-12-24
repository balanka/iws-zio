package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain._
import zio._

trait  PostCustomerInvoice extends PostLogisticalTransaction:
  def postAll(transactions: List[Transaction], company:Company): ZIO[Any, RepositoryError, Int]

object PostCustomerInvoice:
  def postAll(transactions: List[Transaction], company:Company): ZIO[PostCustomerInvoice, RepositoryError, Int] =
    ZIO.serviceWithZIO[PostCustomerInvoice](_.postAll(transactions, company))
