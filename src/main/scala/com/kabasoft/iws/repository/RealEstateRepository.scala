package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.RealEstate
import zio._

trait RealEstateRepository:

  def create(item: RealEstate):ZIO[Any, RepositoryError, Int]
  def create(models: List[RealEstate]):ZIO[Any, RepositoryError, Int]
  def modify(model: RealEstate):ZIO[Any, RepositoryError, Int]
  def modify(models: List[RealEstate]):ZIO[Any, RepositoryError, Int]
  def all(Id: (Int, String)): ZIO[Any, RepositoryError, List[RealEstate]]
  def getById(Id: (String, Int, String)):ZIO[Any, RepositoryError, RealEstate]
  def getBy(ids: List[String], modelid: Int, company: String):ZIO[Any, RepositoryError, List[RealEstate]]
  def delete(p: (String, Int, String)):ZIO[Any, RepositoryError, Int]
  def deleteAll(p: List[(String, Int, String)]): ZIO[Any, RepositoryError, Int] 
  
object RealEstateRepository:

  def create(item: RealEstate): ZIO[RealEstateRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[RealEstateRepository](_.create(item))

  def create(models: List[RealEstate]): ZIO[RealEstateRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[RealEstateRepository](_.create(models))

  def modify(model: RealEstate): ZIO[RealEstateRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[RealEstateRepository](_.modify(model))

  def modify(models: List[RealEstate]): ZIO[RealEstateRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[RealEstateRepository](_.modify(models))

  def all(Id: (Int, String)): ZIO[RealEstateRepository, RepositoryError, List[RealEstate]] =
    ZIO.serviceWithZIO[RealEstateRepository](_.all(Id))

  def getById(Id: (String, Int, String)): ZIO[RealEstateRepository, RepositoryError, RealEstate] =
    ZIO.serviceWithZIO[RealEstateRepository](_.getById(Id))

  def getBy(ids: List[String], modelid: Int, company: String): ZIO[RealEstateRepository, RepositoryError, List[RealEstate]] =
    ZIO.serviceWithZIO[RealEstateRepository](_.getBy(ids, modelid, company))

  def delete(p: (String, Int, String)): ZIO[RealEstateRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[RealEstateRepository](_.delete(p))

  def deleteAll(p: List[(String, Int, String)]): ZIO[RealEstateRepository, RepositoryError, Int] =
     ZIO.serviceWithZIO[RealEstateRepository](_.deleteAll(p))