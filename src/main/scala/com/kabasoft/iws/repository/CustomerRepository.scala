package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Customer
import zio._
import zio.stream._

trait CustomerRepository {
  def create(item: Customer): ZIO[Any, RepositoryError, Customer]
  def create2(item: Customer): ZIO[Any, RepositoryError, Unit]

  def create(models: List[Customer]): ZIO[Any, RepositoryError, List[Customer]]
  def create2(models: List[Customer]): ZIO[Any, RepositoryError, Int]
  def delete(item: String, company: String): ZIO[Any, RepositoryError, Int]
  def delete(items: List[String], company: String): ZIO[Any, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, company)))
  def list(company: String): ZStream[Any, RepositoryError, Customer]
  def all(companyId: String): ZIO[Any, RepositoryError, List[Customer]]
  def getBy(id: (String,  String)): ZIO[Any, RepositoryError, Customer]
  def getByIban(Iban: String, companyId: String): ZIO[Any, RepositoryError, Customer]
  def getByModelId(modelid:(Int, String)): ZIO[Any, RepositoryError, List[Customer]]
  def getByModelIdStream(modelid:Int, companyId:String): ZStream[Any, RepositoryError, Customer]
  def modify(model: Customer): ZIO[Any, RepositoryError, Int]

}
object CustomerRepository {
  def create(item: Customer): ZIO[CustomerRepository, RepositoryError, Customer] =
    ZIO.service[CustomerRepository] flatMap (_.create(item))
  def create2(item: Customer): ZIO[CustomerRepository, RepositoryError, Unit]                               =
    ZIO.service[CustomerRepository] flatMap (_.create2(item))

  def create(items: List[Customer]): ZIO[CustomerRepository, RepositoryError, List[Customer]] =
    ZIO.service[CustomerRepository] flatMap (_.create(items))
  def create2(items: List[Customer]): ZIO[CustomerRepository, RepositoryError, Int]                         =
    ZIO.service[CustomerRepository] flatMap (_.create2(items))
  def delete(item: String, company: String): ZIO[CustomerRepository, RepositoryError, Int]              =
    ZIO.service[CustomerRepository] flatMap (_.delete(item, company))
  def delete(items: List[String], company: String): ZIO[CustomerRepository, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, company)))

  def all(company: String): ZIO[CustomerRepository, RepositoryError, List[Customer]]                      =
    ZIO.service[CustomerRepository] flatMap (_.all(company))
  def getBy(id:(String, String)): ZIO[CustomerRepository, RepositoryError, Customer]              =
    ZIO.service[CustomerRepository] flatMap (_.getBy(id))
  def getByIban(Iban: String, companyId: String): ZIO[CustomerRepository, RepositoryError, Customer]      =
    ZIO.service[CustomerRepository] flatMap (_.getByIban(Iban, companyId))
  def getByModelId(modelid:(Int, String)): ZIO[CustomerRepository, RepositoryError, List[Customer]] =
    ZIO.service[CustomerRepository] flatMap (_.getByModelId(modelid))

  def getByModelIdStream(modelid: Int, company: String): ZStream[CustomerRepository, RepositoryError, Customer] =
    ZStream.service[CustomerRepository] flatMap (_.getByModelIdStream(modelid, company))
  def modify(model: Customer): ZIO[CustomerRepository, RepositoryError, Int]                              =
    ZIO.service[CustomerRepository] flatMap (_.modify(model))

}
