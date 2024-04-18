package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain._
import zio._

trait  PostGoodreceiving extends PostLogisticalTransaction {

  def postAll(transactions: List[Transaction], company:Company): ZIO[Any, RepositoryError, Int]
}
object PostGoodreceiving {
  def postAll(transactions: List[Transaction], company:Company): ZIO[PostGoodreceiving, RepositoryError, Int] =
    ZIO.service[PostGoodreceiving] flatMap (_.postAll(transactions, company))
}
