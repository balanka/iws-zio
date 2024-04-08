package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Fmodule
import zio._
import zio.stream._

trait FModuleRepository {
  def create(item: Fmodule): ZIO[Any, RepositoryError, Fmodule]

  def create(models: List[Fmodule]): ZIO[Any, RepositoryError, List[Fmodule]]
  def create2(item: Fmodule): ZIO[Any, RepositoryError, Unit]
  def create2(models: List[Fmodule]): ZIO[Any, RepositoryError, Int]
  def delete(id: Int, company: String): ZIO[Any, RepositoryError, Int]
  def delete(ids: List[Int], company: String): ZIO[Any, RepositoryError, List[Int]] =
    ZIO.collectAll(ids.map(delete(_, company)))
  def all(Id:(Int, String)): ZIO[Any, RepositoryError, List[Fmodule]]
  def list(Id:(Int, String)): ZStream[Any, RepositoryError, Fmodule]
  def getBy(id: (Int,String)): ZIO[Any, RepositoryError, Fmodule]
  def getByModelId(modelid:(Int,String)): ZIO[Any, RepositoryError, List[Fmodule]]
  def getByModelIdStream(modelid: Int, company: String): ZStream[Any, RepositoryError, Fmodule]
  def modify(model: Fmodule): ZIO[Any, RepositoryError, Int]

}

object FModuleRepository {
  def create(item: Fmodule): ZIO[FModuleRepository, RepositoryError, Fmodule] =
    ZIO.service[FModuleRepository] flatMap (_.create(item))

  def create(items: List[Fmodule]): ZIO[FModuleRepository, RepositoryError, List[Fmodule]] =
    ZIO.service[FModuleRepository] flatMap (_.create(items))
  def create2(item: Fmodule): ZIO[FModuleRepository, RepositoryError, Unit]                               =
    ZIO.service[FModuleRepository] flatMap (_.create2(item))
  def create2(items: List[Fmodule]): ZIO[FModuleRepository, RepositoryError, Int]                         =
    ZIO.service[FModuleRepository] flatMap (_.create2(items))
  def delete(id: Int, company: String): ZIO[FModuleRepository, RepositoryError, Int]              =
    ZIO.service[FModuleRepository] flatMap (_.delete(id, company))
  def delete(items: List[Int], company: String): ZIO[FModuleRepository, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, company)))
  def all(Id:(Int, String)): ZIO[FModuleRepository, RepositoryError, List[Fmodule]]                      =
    ZIO.service[FModuleRepository] flatMap (_.all(Id))
  def list(Id:(Int, String)): ZStream[FModuleRepository, RepositoryError, Fmodule]                        =
    ZStream.service[FModuleRepository] flatMap (_.list(Id))
  def getBy(id: (Int,String)): ZIO[FModuleRepository, RepositoryError, Fmodule]               =
    ZIO.service[FModuleRepository] flatMap (_.getBy(id))
  def getByModelId(modelid: (Int,String)): ZIO[FModuleRepository, RepositoryError, List[Fmodule]]      =
    ZIO.service[FModuleRepository] flatMap (_.getByModelId(modelid))
  def getByModelIdStream(modelid: Int, company: String): ZStream[FModuleRepository, RepositoryError, Fmodule]=
    ZStream.service[FModuleRepository] flatMap (_.getByModelIdStream(modelid, company))
  def modify(model: Fmodule): ZIO[FModuleRepository, RepositoryError, Int]                               =
    ZIO.service[FModuleRepository] flatMap (_.modify(model))

}
