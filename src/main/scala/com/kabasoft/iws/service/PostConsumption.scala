package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain._
import zio._

trait  PostConsumption extends PostLogisticalTransaction:
  def postAll(transactions: List[Transaction], company:Company): ZIO[Any, RepositoryError, Int]

object PostConsumption:
  def postAll(transactions: List[Transaction], company:Company): ZIO[PostConsumption, RepositoryError, Int] =
    ZIO.service[PostConsumption].flatMap (_.postAll(transactions, company))
