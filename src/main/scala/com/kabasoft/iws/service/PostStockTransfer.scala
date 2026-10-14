package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain._
import zio._

trait  PostStockTransfer extends PostLogisticalTransaction:
  def postAll(transactions: List[Transaction], company:Company): ZIO[Any, RepositoryError, Int]

object PostStockTransfer:
  def postAll(transactions: List[Transaction], company:Company): ZIO[PostStockTransfer, RepositoryError, Int] =
    ZIO.service[PostStockTransfer].flatMap (_.postAll(transactions, company))
