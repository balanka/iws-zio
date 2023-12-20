package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Employee
import zio._
import zio.stream._

trait EmployeeRepository {
  def create(item: Employee): ZIO[Any, RepositoryError, Employee]
  def create2(item: Employee): ZIO[Any, RepositoryError, Int]

  def create(models: List[Employee]): ZIO[Any, RepositoryError, List[Employee]]
  def buildInsert(models: List[Employee]): ZIO[Any, RepositoryError, Int]
  def delete(item: String, company: String): ZIO[Any, RepositoryError, Int]
  def delete(items: List[String], company: String): ZIO[Any, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, company)))
  def list(company: String): ZStream[Any, RepositoryError, Employee]
  def all(companyId: String): ZIO[Any, RepositoryError, List[Employee]]
  def getBy(id: (String,  String)): ZIO[Any, RepositoryError, Employee]
  def getByIban(Iban: String, companyId: String): ZIO[Any, RepositoryError, Employee]
  def getByModelId(modelid:(Int, String)): ZIO[Any, RepositoryError, List[Employee]]
  def getByModelIdStream(modelid:Int, companyId:String): ZStream[Any, RepositoryError, Employee]
  def modify(model: Employee): ZIO[Any, RepositoryError, Int]

}
object EmployeeRepository {
  def create(item: Employee): ZIO[EmployeeRepository, RepositoryError, Employee] =
    ZIO.service[EmployeeRepository] flatMap (_.create(item))
  def create2(item: Employee): ZIO[EmployeeRepository, RepositoryError, Int]                               =
    ZIO.service[EmployeeRepository] flatMap (_.create2(item))

  def create(items: List[Employee]): ZIO[EmployeeRepository, RepositoryError, List[Employee]] =
    ZIO.service[EmployeeRepository] flatMap (_.create(items))
  def buildInsert(items: List[Employee]): ZIO[EmployeeRepository, RepositoryError, Int]                         =
    ZIO.service[EmployeeRepository] flatMap (_.buildInsert(items))
  def delete(item: String, company: String): ZIO[EmployeeRepository, RepositoryError, Int]              =
    ZIO.service[EmployeeRepository] flatMap (_.delete(item, company))
  def delete(items: List[String], company: String): ZIO[EmployeeRepository, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, company)))

  def all(company: String): ZIO[EmployeeRepository, RepositoryError, List[Employee]]                      =
    ZIO.service[EmployeeRepository] flatMap (_.all(company))
  def getBy(id:(String, String)): ZIO[EmployeeRepository, RepositoryError, Employee]              =
    ZIO.service[EmployeeRepository] flatMap (_.getBy(id))
  def getByIban(Iban: String, companyId: String): ZIO[EmployeeRepository, RepositoryError, Employee]      =
    ZIO.service[EmployeeRepository] flatMap (_.getByIban(Iban, companyId))
  def getByModelId(modelid:(Int, String)): ZIO[EmployeeRepository, RepositoryError, List[Employee]] =
    ZIO.service[EmployeeRepository] flatMap (_.getByModelId(modelid))

  def getByModelIdStream(modelid: Int, company: String): ZStream[EmployeeRepository, RepositoryError, Employee] =
    ZStream.service[EmployeeRepository] flatMap (_.getByModelIdStream(modelid, company))
  def modify(model: Employee): ZIO[EmployeeRepository, RepositoryError, Int]                              =
    ZIO.service[EmployeeRepository] flatMap (_.modify(model))

}
