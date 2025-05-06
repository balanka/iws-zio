package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{Role, UserRight, UserRole}
import zio._

trait RoleRepository:
  def create(item: Role): ZIO[Any, RepositoryError, Int]
  def create(models: List[Role]):ZIO[Any, RepositoryError, Int]
  def modify(model: Role):ZIO[Any, RepositoryError, Int]
  def modify(models: List[Role]):ZIO[Any, RepositoryError, Int]
  def all(Id: (Int, String)):ZIO[Any, RepositoryError, List[Role]]
  def userRoles(p: (Int, String)): ZIO[Any, RepositoryError, List[UserRole]]
  def allRights(p: (Int, String)):ZIO[Any, RepositoryError, List[UserRight]]
  def userRights(p: (Int, Int, String)):ZIO[Any, RepositoryError, List[UserRight]]
  def getById(Id: (Int, Int, String)):ZIO[Any, RepositoryError, Role]
  def getBy(ids: List[Int], modelid: Int, company: String):ZIO[Any, RepositoryError, List[Role]]
  def delete(p: (Int, Int, String)): ZIO[Any, RepositoryError, Int]
  
object RoleRepository:
    def create(item: Role): ZIO[RoleRepository, RepositoryError, Int] =
      ZIO.serviceWithZIO[RoleRepository](_.create(item))
    def create(models: List[Role]): ZIO[RoleRepository, RepositoryError, Int] =
      ZIO.serviceWithZIO[RoleRepository](_.create(models))
    def modify(model: Role): ZIO[RoleRepository, RepositoryError, Int] =
      ZIO.serviceWithZIO[RoleRepository](_.modify(model))
    def modify(models: List[Role]): ZIO[RoleRepository, RepositoryError, Int] =
      ZIO.serviceWithZIO[RoleRepository](_.modify(models))
    def all(Id: (Int, String)): ZIO[RoleRepository, RepositoryError, List[Role]] =
      ZIO.serviceWithZIO[RoleRepository](_.all(Id))
    def userRoles(p: (Int, String)): ZIO[RoleRepository, RepositoryError, List[UserRole]] =
      ZIO.serviceWithZIO[RoleRepository](_.userRoles(p))
    def allRights(p: (Int, String)): ZIO[RoleRepository, RepositoryError, List[UserRight]] =
       ZIO.serviceWithZIO[RoleRepository](_.allRights(p))
    def userRights(p: (Int, Int, String)): ZIO[RoleRepository, RepositoryError, List[UserRight]] =
      ZIO.serviceWithZIO[RoleRepository](_.userRights(p))  
    def getById(Id: (Int, Int, String)): ZIO[RoleRepository, RepositoryError, Role] =
      ZIO.serviceWithZIO[RoleRepository](_.getById(Id))
    def getBy(ids: List[Int], modelid: Int, company: String): ZIO[RoleRepository, RepositoryError, List[Role]] =
      ZIO.serviceWithZIO[RoleRepository](_.getBy(ids, modelid, company))
    def delete(p: (Int, Int, String)): ZIO[RoleRepository, RepositoryError, Int] =
      ZIO.serviceWithZIO[RoleRepository](_.delete(p))
      