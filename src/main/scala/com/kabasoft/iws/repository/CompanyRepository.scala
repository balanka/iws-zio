package com.kabasoft.iws.repository

import zio.Task
import zio.*
import zio.stream.*
import com.kabasoft.iws.domain.Company
import zio.stream._
import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._

trait CompanyRepository:
  def create(item: Company):ZIO[Any, RepositoryError, Int]
  def create(models: List[Company]):ZIO[Any, RepositoryError, Int]
  def modify(model: Company): ZIO[Any, RepositoryError, Int]
  def modify(models: List[Company]):ZIO[Any, RepositoryError, Int]
  def all(Id:Int): ZIO[Any, RepositoryError, List[Company]]
  def getById(Id: (String, Int)): ZIO[Any, RepositoryError, Company]
  def getBy(ids: List[String], modelid: Int): ZIO[Any, RepositoryError, List[Company]]
  def delete(p: (String, Int)):ZIO[Any, RepositoryError, Int]
  def deleteAll(p:List[(String, Int)]): ZIO[Any, RepositoryError, Int]
  
object CompanyRepository:
  def create(item: Company):ZIO[CompanyRepository, RepositoryError, Int]=
    ZIO.serviceWithZIO[CompanyRepository](_.create(item))
  def create(models: List[Company]): ZIO[CompanyRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[CompanyRepository](_.create(models))
  def modify(model: Company): ZIO[CompanyRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[CompanyRepository](_.modify(model))
  def modify(models: List[Company]): ZIO[CompanyRepository, RepositoryError, Int]=
    ZIO.serviceWithZIO[CompanyRepository](_.modify(models))
  def all(Id:Int): ZIO[CompanyRepository, RepositoryError, List[Company]] =
    ZIO.serviceWithZIO[CompanyRepository](_.all(Id))
  def getById(Id: (String, Int)): ZIO[CompanyRepository, RepositoryError, Company]=
    ZIO.serviceWithZIO[CompanyRepository](_.getById(Id))
  def getBy(ids: List[String], modelid: Int): ZIO[CompanyRepository, RepositoryError, List[Company]]=
    ZIO.serviceWithZIO[CompanyRepository](_.getBy(ids, modelid))
  def delete(p: (String, Int)): ZIO[CompanyRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[CompanyRepository](_.delete(p))
  def deleteAll(p: List[(String, Int)]): ZIO[CompanyRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[CompanyRepository](_.deleteAll(p))  


