package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain._
import zio._

trait  PostGoodreceiving {

  def postAll(transactions: List[Transaction]): ZIO[Any, RepositoryError, Int]
}
object PostGoodreceiving {
  def postAll(transactions: List[Transaction]): ZIO[PostGoodreceiving, RepositoryError, Int] =
    ZIO.service[PostGoodreceiving] flatMap (_.postAll(transactions))
}
