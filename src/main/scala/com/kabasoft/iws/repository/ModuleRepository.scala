package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Module
import zio._
import zio.stream._

trait ModuleRepository {
  def create(item: Module): ZIO[Any, RepositoryError, Unit]
  def create(models: List[Module]): ZIO[Any, RepositoryError, Int]
  def delete(item: String, company: String): ZIO[Any, RepositoryError, Int]
  def delete(items: List[String], company: String): ZIO[Any, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, company)))
  def all(companyId: String): ZIO[Any, RepositoryError, List[Module]]
  def list(company: String): ZStream[Any, RepositoryError, Module]
  def getBy(id: (String,  String)): ZIO[Any, RepositoryError, Module]
  def getByModelId(modelid:(Int, String)): ZIO[Any, RepositoryError, List[Module]]
  def getByModelIdStream(modelid: Int, company: String): ZStream[Any, RepositoryError, Module]
  def modify(model: Module): ZIO[Any, RepositoryError, Int]

}

object ModuleRepository {

  def create(item: Module): ZIO[ModuleRepository, RepositoryError, Unit]                               =
    ZIO.service[ModuleRepository] flatMap (_.create(item))
  def create(items: List[Module]): ZIO[ModuleRepository, RepositoryError, Int]                         =
    ZIO.service[ModuleRepository] flatMap (_.create(items))
  def delete(item: String, company: String): ZIO[ModuleRepository, RepositoryError, Int]              =
    ZIO.service[ModuleRepository] flatMap (_.delete(item, company))
  def delete(items: List[String], company: String): ZIO[ModuleRepository, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, company)))

  def all(companyId: String): ZIO[ModuleRepository, RepositoryError, List[Module]]                =
    ZIO.service[ModuleRepository] flatMap (_.all(companyId))
  def list(company: String): ZStream[ModuleRepository, RepositoryError, Module]                   =
    ZStream.service[ModuleRepository] flatMap (_.list(company))
  def getBy(id:(String,  String)): ZIO[ModuleRepository, RepositoryError, Module]          =
    ZIO.service[ModuleRepository] flatMap (_.getBy(id))
  def getByModelId(modelid:(Int,  String)): ZIO[ModuleRepository, RepositoryError, List[Module]] =
    ZIO.service[ModuleRepository] flatMap (_.getByModelId(modelid))
  def getByModelIdStream(modelid: Int, company: String): ZStream[ModuleRepository, RepositoryError, Module] =
    ZStream.service[ModuleRepository] flatMap (_.getByModelIdStream(modelid, company))
  def modify(model: Module): ZIO[ModuleRepository, RepositoryError, Int]                          =
    ZIO.service[ModuleRepository] flatMap (_.modify(model))

}
