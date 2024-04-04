package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain._
import zio._

trait  PostSupplierInvoice extends PostLogisticalTransaction {

  def postAll(transactions: List[Transaction], company:Company): ZIO[Any, RepositoryError, Int]
}
object PostSupplierInvoice {
  def postAll(transactions: List[Transaction], company:Company): ZIO[PostSupplierInvoice, RepositoryError, Int] =
    ZIO.service[PostSupplierInvoice] flatMap (_.postAll(transactions, company))
}
