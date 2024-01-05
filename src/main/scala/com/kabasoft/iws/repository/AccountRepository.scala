package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.Account
import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._
import zio.stream._

trait AccountRepository {
  def create(item: Account): ZIO[Any, RepositoryError, Account]

  def create(models: List[Account]): ZIO[Any, RepositoryError, List[Account]]
  def create2(item: Account): ZIO[Any, RepositoryError, Unit]
  def create2(models: List[Account]): ZIO[Any, RepositoryError, Int]
  def delete(item: String, company: String): ZIO[Any, RepositoryError, Int]
  def delete(items: List[String], company: String): ZIO[Any, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, company)))
  def list(modelid:Int, company: String): ZStream[Any, RepositoryError, Account]
  def all(Id:(Int, String)): ZIO[Any, RepositoryError, List[Account]]
  def getBy(id: (String,String)): ZIO[Any, RepositoryError, Account]
  def getBy(ids: List[String], company: String): ZIO[Any, RepositoryError, List[Account]]
  def getByModelId(id: (Int,  String)): ZIO[Any, RepositoryError, List[Account]]
  def getByModelIdStream(modelid: Int, company: String): ZStream[Any, RepositoryError, Account]
  def modify(model: Account): ZIO[Any, RepositoryError, Int]
  def modify(models: List[Account]): ZIO[Any, RepositoryError, Int]
}
object AccountRepository {

  def create(item: Account): ZIO[AccountRepository, RepositoryError, Account] =
    ZIO.service[AccountRepository] flatMap (_.create(item))

  def create(items: List[Account]): ZIO[AccountRepository, RepositoryError, List[Account]] =
    ZIO.service[AccountRepository] flatMap (_.create(items))
  def create2(item: Account): ZIO[AccountRepository, RepositoryError, Unit] =
    ZIO.service[AccountRepository] flatMap (_.create2(item))

  def create2(items: List[Account]): ZIO[AccountRepository, RepositoryError, Int] =
    ZIO.service[AccountRepository] flatMap (_.create2(items))

  def delete(item: String, company: String): ZIO[AccountRepository, RepositoryError, Int] =
    ZIO.service[AccountRepository] flatMap (_.delete(item, company))

  def delete(items: List[String], company: String): ZIO[AccountRepository, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, company)))

  def list(modelid:Int, company: String): ZStream[AccountRepository, RepositoryError, Account] =
    ZStream.service[AccountRepository] flatMap (_.list(modelid, company))

  def all(Id:(Int, String)): ZIO[AccountRepository, RepositoryError, List[Account]] =
    ZIO.serviceWithZIO[AccountRepository](_.all(Id))
  def getBy(id: (String,String)): ZIO[AccountRepository, RepositoryError, Account] =
    ZIO.serviceWithZIO[AccountRepository](_.getBy(id))

  def getBy(ids: List[String], companyId: String): ZIO[AccountRepository, RepositoryError, List[Account]] =
    ZIO.serviceWithZIO[AccountRepository](_.getBy(ids, companyId))
  def getByModelId(id: (Int,  String)): ZIO[AccountRepository, RepositoryError, List[Account]] =
    ZIO.serviceWithZIO[AccountRepository](_.getByModelId(id))
  def getByModelIdStream(modelid: Int, company: String): ZStream[AccountRepository, RepositoryError, Account] =
    ZStream.service[AccountRepository] flatMap (_.getByModelIdStream(modelid, company))

  def modify(model: Account): ZIO[AccountRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[AccountRepository](_.modify(model))

  def modify(models: List[Account]): ZIO[AccountRepository, RepositoryError, Int] =
    ZIO.service[AccountRepository] flatMap (_.modify(models))
}
