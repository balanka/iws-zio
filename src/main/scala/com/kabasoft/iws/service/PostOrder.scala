package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain._
import zio._

trait  PostOrder {

  def postAll(transactions: List[Transaction], company:Company): ZIO[Any, RepositoryError, Int]
}
object PostOrder {
  def postAll(transactions: List[Transaction], company:Company): ZIO[PostOrder, RepositoryError, Int] =
    ZIO.service[PostOrder] flatMap (_.postAll(transactions, company))
}
