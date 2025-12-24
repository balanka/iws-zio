package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain._
import zio._

trait  PostBillOfDelivery extends PostLogisticalTransaction:
  def postAll(transactions: List[Transaction], company:Company): ZIO[Any, RepositoryError, Int]

object PostBillOfDelivery:
  def postAll(transactions: List[Transaction], company:Company): ZIO[PostBillOfDelivery, RepositoryError, Int] =
    ZIO.serviceWithZIO[PostBillOfDelivery](_.postAll(transactions, company))