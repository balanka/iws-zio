package com.kabasoft.iws.repository
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Fmodule
import zio.Task
import zio.*
import zio.stream.*

import java.time.LocalDate

trait FModuleRepository:

  def create(item: Fmodule, flag: Boolean): ZIO[Any, RepositoryError, Int]

  def create(models: List[Fmodule]):ZIO[Any, RepositoryError, Int]

  def modify(model: Fmodule):ZIO[Any, RepositoryError, Int]

  def modify(models: List[Fmodule]):ZIO[Any, RepositoryError, Int]

  def all(Id: (Int, String)): ZIO[Any, RepositoryError, List[Fmodule]]
  def getById(Id: (Int, Int, String)):ZIO[Any, RepositoryError, Fmodule]
  def getBy(ids: List[Int], modelid: Int, company: String):ZIO[Any, RepositoryError, List[Fmodule]]

  def delete(p: (Int, Int, String)): ZIO[Any, RepositoryError, Int]

object FModuleRepository:

  def create(item: Fmodule, flag: Boolean):ZIO[FModuleRepository, RepositoryError, Int]=
    ZIO.serviceWithZIO[FModuleRepository](_.create(item, flag))

  def create(models: List[Fmodule]): ZIO[FModuleRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[FModuleRepository](_.create(models))

  def modify(model: Fmodule): ZIO[FModuleRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[FModuleRepository](_.modify(model))

  def modify(models: List[Fmodule]): ZIO[FModuleRepository, RepositoryError, Int]=
    ZIO.serviceWithZIO[FModuleRepository](_.modify(models))

  def all(Id: (Int, String)): ZIO[FModuleRepository, RepositoryError, List[Fmodule]] =
    ZIO.serviceWithZIO[FModuleRepository](_.all(Id))

  def getById(Id: (Int, Int, String)): ZIO[FModuleRepository, RepositoryError, Fmodule]=
    ZIO.serviceWithZIO[FModuleRepository](_.getById(Id))

  def getBy(ids: List[Int], modelid: Int, company: String): ZIO[FModuleRepository, RepositoryError, List[Fmodule]]=
    ZIO.serviceWithZIO[FModuleRepository](_.getBy(ids, modelid, company))

  def delete(p: (Int, Int, String)): ZIO[FModuleRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[FModuleRepository](_.delete(p))