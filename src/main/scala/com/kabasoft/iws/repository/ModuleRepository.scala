package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Module
import zio._

trait ModuleRepository:

  def create(item: Module): ZIO[Any, RepositoryError, Int]

  def create(models: List[Module]):ZIO[Any, RepositoryError, Int]

  def modify(model: Module):ZIO[Any, RepositoryError, Int]

  def modify(models: List[Module]):ZIO[Any, RepositoryError, Int]

  def all(Id: (Int, String)):ZIO[Any, RepositoryError, List[Module]]

  def getBy(ids: List[String], modelidd:Int, comapany:String):ZIO[Any, RepositoryError, List[Module]]

  def getById( p: (String, Int, String)): ZIO[Any, RepositoryError, Module]

  def delete(p: (String, Int, String)): ZIO[Any, RepositoryError, Int]

object ModuleRepository:

  def create(item: Module): ZIO[ModuleRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[ModuleRepository](_.create(item))

  def create(models: List[Module]): ZIO[ModuleRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[ModuleRepository](_.create(models))

  def modify(model: Module): ZIO[ModuleRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[ModuleRepository](_.modify(model))

  def modify(models: List[Module]): ZIO[ModuleRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[ModuleRepository](_.modify(models))

  def all(Id: (Int, String)): ZIO[ModuleRepository, RepositoryError, List[Module]] =
    ZIO.serviceWithZIO[ModuleRepository](_.all(Id))

  def getBy(Ids: List[String], modelid: Int, companyId: String): ZIO[ModuleRepository, RepositoryError, List[Module]] =
    ZIO.serviceWithZIO[ModuleRepository](_.getBy(Ids, modelid, companyId))

  def getById(p: (String, Int, String)): ZIO[ModuleRepository, RepositoryError, Module] =
    ZIO.serviceWithZIO[ModuleRepository](_.getById(p))

  def delete(p: (String, Int, String)): ZIO[ModuleRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[ModuleRepository](_.delete(p))
  