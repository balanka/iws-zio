package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.Company
import zio.stream._
import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._

trait CompanyRepository {
  type TYPE_ = Company
  def create(item: TYPE_): ZIO[Any, RepositoryError, Unit]
  def create(models: List[TYPE_]): ZIO[Any, RepositoryError, Int]
  def delete(item: String): ZIO[Any, RepositoryError, Int]
  def delete(items: List[String]): ZIO[Any, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_)))
  def all: ZIO[Any, RepositoryError, List[TYPE_]]
  def list: ZStream[Any, RepositoryError, TYPE_]
  def getBy(id: String): ZIO[Any, RepositoryError, TYPE_]
  def modify(model: TYPE_): ZIO[Any, RepositoryError, Int]

}

object CompanyRepository {

  type TYPE_ = Company
  def create(item: TYPE_): ZIO[CompanyRepository, RepositoryError, Unit]              =
    ZIO.service[CompanyRepository] flatMap (_.create(item))
  def create(items: List[TYPE_]): ZIO[CompanyRepository, RepositoryError, Int]        =
    ZIO.service[CompanyRepository] flatMap (_.create(items))
  def delete(item: String): ZIO[CompanyRepository, RepositoryError, Int]              =
    ZIO.service[CompanyRepository] flatMap (_.delete(item))
  def delete(items: List[String]): ZIO[CompanyRepository, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_)))

  def all: ZIO[CompanyRepository, RepositoryError, List[TYPE_]] =
    ZIO.service[CompanyRepository] flatMap (_.all)

  def list: ZStream[CompanyRepository, RepositoryError, TYPE_]           =
    ZStream.service[CompanyRepository] flatMap (_.list)
  def getBy(id: String): ZIO[CompanyRepository, RepositoryError, TYPE_]  =
    ZIO.service[CompanyRepository] flatMap (_.getBy(id))
  def modify(model: TYPE_): ZIO[CompanyRepository, RepositoryError, Int] =
    ZIO.service[CompanyRepository] flatMap (_.modify(model))

}
