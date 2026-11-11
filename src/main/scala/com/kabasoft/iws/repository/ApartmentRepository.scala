package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Apartment
import zio._

trait ApartmentRepository:

  def create(item: Apartment):ZIO[Any, RepositoryError, Int]
  def create(models: List[Apartment]):ZIO[Any, RepositoryError, Int]
  def modify(model: Apartment):ZIO[Any, RepositoryError, Int]
  def modify(models: List[Apartment]):ZIO[Any, RepositoryError, Int]
  def all(Id: (Int, String)): ZIO[Any, RepositoryError, List[Apartment]]
  def getById(Id: (String, Int, String)):ZIO[Any, RepositoryError, Apartment]
  def getByParent(parent: String, modelid: Int, company: String): ZIO[Any, RepositoryError, List[Apartment]]
  def getBy(ids: List[String], modelid: Int, company: String):ZIO[Any, RepositoryError, List[Apartment]]
  def delete(p: (String, Int, String)):ZIO[Any, RepositoryError, Int]
  def deleteAll(p: List[(String, Int, String)]): ZIO[Any, RepositoryError, Int] 
  
object ApartmentRepository:

  def create(item: Apartment): ZIO[ApartmentRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[ApartmentRepository](_.create(item))

  def create(models: List[Apartment]): ZIO[ApartmentRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[ApartmentRepository](_.create(models))

  def modify(model: Apartment): ZIO[ApartmentRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[ApartmentRepository](_.modify(model))

  def modify(models: List[Apartment]): ZIO[ApartmentRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[ApartmentRepository](_.modify(models))

  def all(Id: (Int, String)): ZIO[ApartmentRepository, RepositoryError, List[Apartment]] =
    ZIO.serviceWithZIO[ApartmentRepository](_.all(Id))

  def getById(Id: (String, Int, String)): ZIO[ApartmentRepository, RepositoryError, Apartment] =
    ZIO.serviceWithZIO[ApartmentRepository](_.getById(Id))

  def getByParent(parent: String, modelid: Int, company: String): ZIO[ApartmentRepository, RepositoryError, List[Apartment]] =
    ZIO.serviceWithZIO[ApartmentRepository](_.getByParent(parent, modelid, company))
    
  def getBy(ids: List[String], modelid: Int, company: String): ZIO[ApartmentRepository, RepositoryError, List[Apartment]] =
    ZIO.serviceWithZIO[ApartmentRepository](_.getBy(ids, modelid, company))

  def delete(p: (String, Int, String)): ZIO[ApartmentRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[ApartmentRepository](_.delete(p))

  def deleteAll(p: List[(String, Int, String)]): ZIO[ApartmentRepository, RepositoryError, Int] =
     ZIO.serviceWithZIO[ApartmentRepository](_.deleteAll(p))