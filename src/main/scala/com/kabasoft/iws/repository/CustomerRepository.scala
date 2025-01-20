package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Customer
import zio.Task
import zio.*
import zio.stream.*

import java.time.LocalDate

trait CustomerRepository:

  def create(item: Customer):ZIO[Any, RepositoryError, Int]

  def create(models: List[Customer]):ZIO[Any, RepositoryError, Int]

  def modify(model: Customer):ZIO[Any, RepositoryError, Int]

  def modify(models: List[Customer]):ZIO[Any, RepositoryError, Int]

  def all(Id: (Int, String)): ZIO[Any, RepositoryError, List[Customer]]

  def getById(Id: (String, Int, String)):ZIO[Any, RepositoryError, Customer]

  def getByIban(Id: (String, Int, String)):ZIO[Any, RepositoryError, Customer]

  def getBy(ids: List[String], modelid: Int, company: String):ZIO[Any, RepositoryError, List[Customer]]

  def delete(p: (String, Int, String)):ZIO[Any, RepositoryError, Int]
  def deleteAll(p: List[(String, Int, String)]): ZIO[Any, RepositoryError, Int] 
  
object CustomerRepository:

  def create(item: Customer): ZIO[CustomerRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[CustomerRepository](_.create(item))

  def create(models: List[Customer]): ZIO[CustomerRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[CustomerRepository](_.create(models))

  def modify(model: Customer): ZIO[CustomerRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[CustomerRepository](_.modify(model))

  def modify(models: List[Customer]): ZIO[CustomerRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[CustomerRepository](_.modify(models))

  def all(Id: (Int, String)): ZIO[CustomerRepository, RepositoryError, List[Customer]] =
    ZIO.serviceWithZIO[CustomerRepository](_.all(Id))

  def getById(Id: (String, Int, String)): ZIO[CustomerRepository, RepositoryError, Customer] =
    ZIO.serviceWithZIO[CustomerRepository](_.getById(Id))
    
  def getByIban(Id: (String, Int, String)): ZIO[CustomerRepository, RepositoryError, Customer] =
    ZIO.serviceWithZIO[CustomerRepository](_.getByIban(Id))

  def getBy(ids: List[String], modelid: Int, company: String): ZIO[CustomerRepository, RepositoryError, List[Customer]] =
    ZIO.serviceWithZIO[CustomerRepository](_.getBy(ids, modelid, company))

  def delete(p: (String, Int, String)): ZIO[CustomerRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[CustomerRepository](_.delete(p))

  def deleteAll(p: List[(String, Int, String)]): ZIO[CustomerRepository, RepositoryError, Int] =
     ZIO.serviceWithZIO[CustomerRepository](_.deleteAll(p))