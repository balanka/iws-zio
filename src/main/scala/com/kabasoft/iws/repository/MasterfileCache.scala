package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.Masterfile
import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._


trait MasterfileCache {
  def all(Id:(Int, String)): ZIO[Any, RepositoryError, List[Masterfile]]
  def getBy(id:(String, Int,  String)): ZIO[Any, RepositoryError, Masterfile]
  def getByModelId(id:(Int, String)): ZIO[Any, RepositoryError, List[Masterfile]]

}
object MasterfileCache {
  def all(Id:(Int, String)): ZIO[MasterfileCache, RepositoryError, List[Masterfile]] =
    ZIO.service[MasterfileCache] flatMap (_.all(Id))

  def getBy(id:(String, Int, String)): ZIO[MasterfileCache, RepositoryError, Masterfile]=
    ZIO.service[MasterfileCache] flatMap (_.getBy(id))

  def getByModelId(id:(Int, String)): ZIO[MasterfileCache, RepositoryError, List[Masterfile]] =
    ZIO.service[MasterfileCache] flatMap (_.getByModelId(id))

}
