package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Masterfile
import zio._
import zio.stream._

trait MasterfileRepository {
  def create(item: Masterfile): ZIO[Any, RepositoryError, Masterfile]
  def create(models: List[Masterfile]): ZIO[Any, RepositoryError, List[Masterfile]]
  def create2(item: Masterfile): ZIO[Any, RepositoryError, Unit]
  def create2(models: List[Masterfile]): ZIO[Any, RepositoryError, Int]
  def delete(item: String, modelId:Int, company: String): ZIO[Any, RepositoryError, Int]
  def delete(items: List[String], modelId:Int, company: String): ZIO[Any, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, modelId, company)))
  def all(Id:(Int, String)): ZIO[Any, RepositoryError, List[Masterfile]]
  def list(Id:(Int, String)): ZStream[Any, RepositoryError, Masterfile]
  def getBy(id: (String, Int,  String)): ZIO[Any, RepositoryError, Masterfile]
  def getByModelId(modelid:(Int, String)): ZIO[Any, RepositoryError, List[Masterfile]]
  def getByModelIdStream(modelid: Int, company: String): ZStream[Any, RepositoryError, Masterfile]
  def modify(model: Masterfile): ZIO[Any, RepositoryError, Int]

}

object MasterfileRepository {
  def create(item: Masterfile): ZIO[MasterfileRepository, RepositoryError, Masterfile] =
    ZIO.service[MasterfileRepository] flatMap (_.create(item))

  def create(items: List[Masterfile]): ZIO[MasterfileRepository, RepositoryError, List[Masterfile]] =
    ZIO.service[MasterfileRepository] flatMap (_.create(items))
  def create2(item: Masterfile): ZIO[MasterfileRepository, RepositoryError, Unit]                               =
    ZIO.service[MasterfileRepository] flatMap (_.create2(item))
  def create2(items: List[Masterfile]): ZIO[MasterfileRepository, RepositoryError, Int]                         =
    ZIO.service[MasterfileRepository] flatMap (_.create2(items))
  def delete(item: String, modelId:Int, company: String): ZIO[MasterfileRepository, RepositoryError, Int]              =
    ZIO.service[MasterfileRepository] flatMap (_.delete(item, modelId, company))
  def delete(items: List[String], modelId:Int, company: String): ZIO[MasterfileRepository, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, modelId, company)))

  def all(Id:(Int, String)): ZIO[MasterfileRepository, RepositoryError, List[Masterfile]]                =
    ZIO.service[MasterfileRepository] flatMap (_.all(Id))
  def list(Id:(Int, String)): ZStream[MasterfileRepository, RepositoryError, Masterfile]                   =
    ZStream.service[MasterfileRepository] flatMap (_.list(Id))
  def getBy(id:(String,Int,  String)): ZIO[MasterfileRepository, RepositoryError, Masterfile]          =
    ZIO.service[MasterfileRepository] flatMap (_.getBy(id))
  def getByModelId(modelid:(Int,  String)): ZIO[MasterfileRepository, RepositoryError, List[Masterfile]] =
    ZIO.service[MasterfileRepository] flatMap (_.getByModelId(modelid))
  def getByModelIdStream(modelid: Int, company: String): ZStream[MasterfileRepository, RepositoryError, Masterfile] =
    ZStream.service[MasterfileRepository] flatMap (_.getByModelIdStream(modelid, company))
  def modify(model: Masterfile): ZIO[MasterfileRepository, RepositoryError, Int]                          =
    ZIO.service[MasterfileRepository] flatMap (_.modify(model))

}
