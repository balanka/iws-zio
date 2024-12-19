package com.kabasoft.iws.repository
import com.kabasoft.iws.domain.Account
import com.kabasoft.iws.domain.AppError.RepositoryError
import zio.*

trait AccountRepository:
  def create(item: Account):ZIO[Any, RepositoryError, Int]
  def create(models: List[Account]):ZIO[Any, RepositoryError, Int]
  def modify(model: Account):ZIO[Any, RepositoryError, Int]
  def modify(models: List[Account]):ZIO[Any, RepositoryError, Int]
  def all(Id: (Int, String)): ZIO[Any, RepositoryError, List[Account]]
  def getById(Id: (String, Int, String)):ZIO[Any, RepositoryError, Account]
  def getBy(ids: List[String], modelid: Int, company: String): ZIO[Any, RepositoryError, List[Account]]
  def delete(p: (String, Int, String)): ZIO[Any, RepositoryError, Int]

object AccountRepository:

  def create(item: Account):ZIO[AccountRepository, RepositoryError, Int]=
    ZIO.serviceWithZIO[AccountRepository](_.create(item))

  def create(models: List[Account]): ZIO[AccountRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[AccountRepository](_.create(models))

  def modify(model: Account): ZIO[AccountRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[AccountRepository](_.modify(model))

  def modify(models: List[Account]): ZIO[AccountRepository, RepositoryError, Int]=
    ZIO.serviceWithZIO[AccountRepository](_.modify(models))

  def all(Id: (Int, String)): ZIO[AccountRepository, RepositoryError, List[Account]] =
    ZIO.serviceWithZIO[AccountRepository](_.all(Id))

  def getById(Id: (String, Int, String)): ZIO[AccountRepository, RepositoryError, Account]=
    ZIO.serviceWithZIO[AccountRepository](_.getById(Id))

  def getBy(ids: List[String], modelid: Int, company: String): ZIO[AccountRepository, RepositoryError, List[Account]]=
    ZIO.serviceWithZIO[AccountRepository](_.getBy(ids, modelid, company))

  def delete(p: (String, Int, String)): ZIO[AccountRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[AccountRepository](_.delete(p))