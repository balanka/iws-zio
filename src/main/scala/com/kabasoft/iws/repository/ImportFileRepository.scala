package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.ImportFile
import zio._
import zio.stream._

trait ImportFileRepository {
  def create(item: ImportFile): ZIO[Any, RepositoryError, ImportFile]

  def create(models: List[ImportFile]): ZIO[Any, RepositoryError, List[ImportFile]]
  def create2(item: ImportFile): ZIO[Any, RepositoryError, Unit]
  def create2(models: List[ImportFile]): ZIO[Any, RepositoryError, Int]
  def delete(item: String, company: String): ZIO[Any, RepositoryError, Int]
  def delete(items: List[String], company: String): ZIO[Any, RepositoryError, List[Int]] =
    ZIO.foreach(items)(delete(_, company))
  def all(companyId: String): ZIO[Any, RepositoryError, List[ImportFile]]
  def list(company: String): ZStream[Any, RepositoryError, ImportFile]
  def getBy(id: (String,String)): ZIO[Any, RepositoryError, ImportFile]
  def getByModelId(modelid:(Int,String)): ZIO[Any, RepositoryError, List[ImportFile]]
  def getByModelIdStream(modelid: Int, company: String): ZStream[Any, RepositoryError, ImportFile]
  def modify(model: ImportFile): ZIO[Any, RepositoryError, Int]

}

object ImportFileRepository {
  def create(item: ImportFile): ZIO[ImportFileRepository, RepositoryError, ImportFile] =
    ZIO.serviceWithZIO[ImportFileRepository](_.create(item))

  def create(items: List[ImportFile]): ZIO[ImportFileRepository, RepositoryError, List[ImportFile]] =
    ZIO.serviceWithZIO[ImportFileRepository](_.create(items))
  def create2(item: ImportFile): ZIO[ImportFileRepository, RepositoryError, Unit]                               =
    ZIO.serviceWithZIO[ImportFileRepository](_.create2(item))
  def create2(items: List[ImportFile]): ZIO[ImportFileRepository, RepositoryError, Int]                         =
    ZIO.serviceWithZIO[ImportFileRepository](_.create2(items))
  def delete(item: String, company: String): ZIO[ImportFileRepository, RepositoryError, Int]              =
    ZIO.serviceWithZIO[ImportFileRepository](_.delete(item, company))
  def delete(items: List[String], company: String): ZIO[ImportFileRepository, RepositoryError, List[Int]] =
    ZIO.foreach(items)(delete(_, company))
  def all(companyId: String): ZIO[ImportFileRepository, RepositoryError, List[ImportFile]]                      =
    ZIO.serviceWithZIO[ImportFileRepository](_.all(companyId))
  def list(company: String): ZStream[ImportFileRepository, RepositoryError, ImportFile]                        =
    ZStream.service[ImportFileRepository] flatMap (_.list(company))
  def getBy(id: (String,String)): ZIO[ImportFileRepository, RepositoryError, ImportFile]               =
    ZIO.serviceWithZIO[ImportFileRepository](_.getBy(id))
  def getByModelId(modelid: (Int,String)): ZIO[ImportFileRepository, RepositoryError, List[ImportFile]]      =
    ZIO.serviceWithZIO[ImportFileRepository](_.getByModelId(modelid))
  def getByModelIdStream(modelid: Int, company: String): ZStream[ImportFileRepository, RepositoryError, ImportFile]=
    ZStream.service[ImportFileRepository] flatMap (_.getByModelIdStream(modelid, company))
  def modify(model: ImportFile): ZIO[ImportFileRepository, RepositoryError, Int]                               =
    ZIO.serviceWithZIO[ImportFileRepository](_.modify(model))

}
