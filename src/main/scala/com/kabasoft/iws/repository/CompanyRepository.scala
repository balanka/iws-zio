package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.Company
import zio.stream._
import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._

trait CompanyRepository {
  def create(item: Company): ZIO[Any, RepositoryError, Company]

  def create(models: List[Company]): ZIO[Any, RepositoryError, List[Company]]
  def create2(item: Company): ZIO[Any, RepositoryError, Unit]
  def create2(models: List[Company]): ZIO[Any, RepositoryError, Int]
  def delete(item: String): ZIO[Any, RepositoryError, Int]
  def delete(items: List[String]): ZIO[Any, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_)))
  def all: ZIO[Any, RepositoryError, List[Company]]
  def list: ZStream[Any, RepositoryError, Company]
  def getBy(id: String): ZIO[Any, RepositoryError, Company]
  def modify(model: Company): ZIO[Any, RepositoryError, Int]

}

object CompanyRepository {
  def create(item: Company): ZIO[CompanyRepository, RepositoryError, Company] =
    ZIO.service[CompanyRepository] flatMap (_.create(item))

  def create(items: List[Company]): ZIO[CompanyRepository, RepositoryError, List[Company]] =
    ZIO.service[CompanyRepository] flatMap (_.create(items))
  def create2(item: Company): ZIO[CompanyRepository, RepositoryError, Unit]              =
    ZIO.service[CompanyRepository] flatMap (_.create2(item))
  def create2(items: List[Company]): ZIO[CompanyRepository, RepositoryError, Int]        =
    ZIO.service[CompanyRepository] flatMap (_.create2(items))
  def delete(item: String): ZIO[CompanyRepository, RepositoryError, Int]              =
    ZIO.service[CompanyRepository] flatMap (_.delete(item))
  def delete(items: List[String]): ZIO[CompanyRepository, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete))

  def all: ZIO[CompanyRepository, RepositoryError, List[Company]] =
    ZIO.service[CompanyRepository] flatMap (_.all)

  def list: ZStream[CompanyRepository, RepositoryError, Company]           =
    ZStream.service[CompanyRepository] flatMap (_.list)
  def getBy(id: String): ZIO[CompanyRepository, RepositoryError, Company]  =
    ZIO.service[CompanyRepository] flatMap (_.getBy(id))
  def modify(model: Company): ZIO[CompanyRepository, RepositoryError, Int] =
    ZIO.service[CompanyRepository] flatMap (_.modify(model))

}
