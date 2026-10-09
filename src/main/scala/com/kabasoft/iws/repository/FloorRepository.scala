package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Floor
import zio._

trait FloorRepository:

  def create(item: Floor):ZIO[Any, RepositoryError, Int]
  def create(models: List[Floor]):ZIO[Any, RepositoryError, Int]
  def modify(model: Floor):ZIO[Any, RepositoryError, Int]
  def modify(models: List[Floor]):ZIO[Any, RepositoryError, Int]
  def all(Id: (Int, String)): ZIO[Any, RepositoryError, List[Floor]]
  def getById(Id: (String, Int, String)):ZIO[Any, RepositoryError, Floor]
  def getByParent(parent: String, modelid: Int, company: String): ZIO[Any, RepositoryError, List[Floor]]
  def getBy(ids: List[String], modelid: Int, company: String):ZIO[Any, RepositoryError, List[Floor]]
  def delete(p: (String, Int, String)):ZIO[Any, RepositoryError, Int]
  def deleteAll(p: List[(String, Int, String)]): ZIO[Any, RepositoryError, Int] 
  
object FloorRepository:

  def create(item: Floor): ZIO[FloorRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[FloorRepository](_.create(item))

  def create(models: List[Floor]): ZIO[FloorRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[FloorRepository](_.create(models))

  def modify(model: Floor): ZIO[FloorRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[FloorRepository](_.modify(model))

  def modify(models: List[Floor]): ZIO[FloorRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[FloorRepository](_.modify(models))

  def all(Id: (Int, String)): ZIO[FloorRepository, RepositoryError, List[Floor]] =
    ZIO.serviceWithZIO[FloorRepository](_.all(Id))

  def getById(Id: (String, Int, String)): ZIO[FloorRepository, RepositoryError, Floor] =
    ZIO.serviceWithZIO[FloorRepository](_.getById(Id))

  def getByParent(parent: String, modelid: Int, company: String): ZIO[FloorRepository, RepositoryError, List[Floor]] =
    ZIO.serviceWithZIO[FloorRepository](_.getByParent(parent, modelid, company))
    
  def getBy(ids: List[String], modelid: Int, company: String): ZIO[FloorRepository, RepositoryError, List[Floor]] =
    ZIO.serviceWithZIO[FloorRepository](_.getBy(ids, modelid, company))

  def delete(p: (String, Int, String)): ZIO[FloorRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[FloorRepository](_.delete(p))

  def deleteAll(p: List[(String, Int, String)]): ZIO[FloorRepository, RepositoryError, Int] =
     ZIO.serviceWithZIO[FloorRepository](_.deleteAll(p))