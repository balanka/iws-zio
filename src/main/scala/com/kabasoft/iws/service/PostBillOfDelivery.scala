package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain._
import com.kabasoft.iws.repository._
import zio._

trait  PostBillOfDelivery {

  def postAll(transactions: List[Transaction]): ZIO[Any, RepositoryError, Int]
}
object PostBillOfDelivery {
  def postAll(transactions: List[Transaction]): ZIO[PostBillOfDelivery, RepositoryError, Int] =
    ZIO.service[PostBillOfDelivery] flatMap (_.postAll(transactions))
}
