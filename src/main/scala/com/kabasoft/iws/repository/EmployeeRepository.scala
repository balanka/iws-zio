package com.kabasoft.iws.repository
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Employee
import zio.Task
import zio.*
import zio.stream.*

import java.time.LocalDate

trait EmployeeRepository:

  def create(item: Employee): ZIO[Any, RepositoryError, Int]

  def create(models: List[Employee]):ZIO[Any, RepositoryError, Int]

  def modify(model: Employee):ZIO[Any, RepositoryError, Int]

  def modify(models: List[Employee]):ZIO[Any, RepositoryError, Int]

  def all(Id: (Int, String)): ZIO[Any, RepositoryError, List[Employee]]

  def getById(Id: (String, Int, String)): ZIO[Any, RepositoryError, Employee]

  def getBy(ids: List[String], modelid: Int, company: String):ZIO[Any, RepositoryError, List[Employee]]

  def delete(p: (String, Int, String)):ZIO[Any, RepositoryError, Int]

object EmployeeRepository:

  def create(item: Employee): ZIO[EmployeeRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[EmployeeRepository](_.create(item))

  def create(models: List[Employee]): ZIO[EmployeeRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[EmployeeRepository](_.create(models))

  def modify(model: Employee): ZIO[EmployeeRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[EmployeeRepository](_.modify(model))

  def modify(models: List[Employee]): ZIO[EmployeeRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[EmployeeRepository](_.modify(models))

  def all(Id: (Int, String)): ZIO[EmployeeRepository, RepositoryError, List[Employee]] =
    ZIO.serviceWithZIO[EmployeeRepository](_.all(Id))

  def getById(Id: (String, Int, String)): ZIO[EmployeeRepository, RepositoryError, Employee] =
    ZIO.serviceWithZIO[EmployeeRepository](_.getById(Id))

  def getBy(ids: List[String], modelid: Int, company: String): ZIO[EmployeeRepository, RepositoryError, List[Employee]] =
    ZIO.serviceWithZIO[EmployeeRepository](_.getBy(ids, modelid, company))

  def delete(p: (String, Int, String)): ZIO[EmployeeRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[EmployeeRepository](_.delete(p))
    
    