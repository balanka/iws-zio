package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Permission
import zio.Task
import zio.*
import zio.stream.*

import java.time.LocalDate

trait PermissionRepository:

  def create(item: Permission, flag: Boolean): ZIO[Any, RepositoryError, Int]

  def create(models: List[Permission]):ZIO[Any, RepositoryError, Int]

  def modify(model: Permission):ZIO[Any, RepositoryError, Int]

  def modify(models: List[Permission]):ZIO[Any, RepositoryError, Int]

  def all(Id: (Int, String)): ZIO[Any, RepositoryError, List[Permission]]
  def getById(Id: (Int, Int, String)):ZIO[Any, RepositoryError, Permission]
  def getBy(ids: List[Int], modelid: Int, company: String):ZIO[Any, RepositoryError, List[Permission]]

  def delete(p: (Int, Int, String)):ZIO[Any, RepositoryError, Int]

object PermissionRepository:

  def create(item: Permission, flag: Boolean):ZIO[PermissionRepository, RepositoryError, Int]=
    ZIO.serviceWithZIO[PermissionRepository](_.create(item, flag))

  def create(models: List[Permission]): ZIO[PermissionRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[PermissionRepository](_.create(models))

  def modify(model: Permission): ZIO[PermissionRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[PermissionRepository](_.modify(model))

  def modify(models: List[Permission]): ZIO[PermissionRepository, RepositoryError, Int]=
    ZIO.serviceWithZIO[PermissionRepository](_.modify(models))

  def all(Id: (Int, String)): ZIO[PermissionRepository, RepositoryError, List[Permission]] =
    ZIO.serviceWithZIO[PermissionRepository](_.all(Id))

  def getById(Id: (Int, Int, String)): ZIO[PermissionRepository, RepositoryError, Permission]=
    ZIO.serviceWithZIO[PermissionRepository](_.getById(Id))

  def getBy(ids: List[Int], modelid: Int, company: String): ZIO[PermissionRepository, RepositoryError, List[Permission]]=
    ZIO.serviceWithZIO[PermissionRepository](_.getBy(ids, modelid, company))

  def delete(p: (Int, Int, String)): ZIO[PermissionRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[PermissionRepository](_.delete(p))