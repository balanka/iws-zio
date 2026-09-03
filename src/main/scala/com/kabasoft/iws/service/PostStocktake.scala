package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain._
import zio._

trait  PostStocktake extends PostStocktakeOrConsumption:
  def postAll(transactions: List[Transaction], company:Company): ZIO[Any, RepositoryError, Int]

object PostStocktake:
  def postAll(transactions: List[Transaction], company:Company): ZIO[PostStocktake, RepositoryError, Int] =
    ZIO.service[PostStocktake].flatMap (_.postAll(transactions, company))
