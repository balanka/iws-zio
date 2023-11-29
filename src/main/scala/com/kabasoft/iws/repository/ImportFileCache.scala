package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.ImportFile
import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._


trait ImportFileCache  {
  def all(companyId: String): ZIO[Any, RepositoryError, List[ImportFile]]
  def getBy(id:(String,  String)): ZIO[Any, RepositoryError, ImportFile]
  def getByModelId(id:(Int, String)): ZIO[Any, RepositoryError, List[ImportFile]]

}
object ImportFileCache {
  def all(companyId: String): ZIO[ImportFileCache, RepositoryError, List[ImportFile]] =
    ZIO.service[ImportFileCache] flatMap (_.all(companyId))

  def getBy(id:(String, String)): ZIO[ImportFileCache, RepositoryError, ImportFile]=
    ZIO.service[ImportFileCache] flatMap (_.getBy(id))

  def getByModelId(id:(Int, String)): ZIO[ImportFileCache, RepositoryError, List[ImportFile]] =
    ZIO.service[ImportFileCache] flatMap (_.getByModelId(id))

}
