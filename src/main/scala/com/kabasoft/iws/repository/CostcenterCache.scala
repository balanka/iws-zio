package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.Costcenter
import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._


trait CostcenterCache {
  def all(companyId: String): ZIO[Any, RepositoryError, List[Costcenter]]
  def getBy(id:(String,  String)): ZIO[Any, RepositoryError, Costcenter]
  def getByModelId(id:(Int, String)): ZIO[Any, RepositoryError, List[Costcenter]]

}
object CostcenterCache {
  def all(companyId: String): ZIO[CostcenterCache, RepositoryError, List[Costcenter]] =
    ZIO.service[CostcenterCache] flatMap (_.all(companyId))

  def getBy(id:(String, String)): ZIO[CostcenterCache, RepositoryError, Costcenter]=
    ZIO.service[CostcenterCache] flatMap (_.getBy(id))

  def getByModelId(id:(Int, String)): ZIO[CostcenterCache, RepositoryError, List[Costcenter]] =
    ZIO.service[CostcenterCache] flatMap (_.getByModelId(id))

}