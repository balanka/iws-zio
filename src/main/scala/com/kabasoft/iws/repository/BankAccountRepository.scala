package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.BankAccount
import zio._

trait BankAccountRepository:
  def create(item: BankAccount): ZIO[Any, RepositoryError, Int]
  def create(models: List[BankAccount]): ZIO[Any, RepositoryError, Int]
  def modify(model: BankAccount): ZIO[Any, RepositoryError, Int]
  def modify(models: List[BankAccount]):ZIO[Any, RepositoryError, Int]
  def bankAccout4All(modelId:Int): ZIO[Any, RepositoryError, List[BankAccount]]
  def all(Id: (Int, String)): ZIO[Any, RepositoryError, List[BankAccount]]
  def getById(Id: (String, Int, String)):ZIO[Any, RepositoryError, BankAccount]
  def getBy(ids: List[String], modelid: Int, company: String):ZIO[Any, RepositoryError, List[BankAccount]]
  def delete(p: (String, Int, String)): ZIO[Any, RepositoryError, Int]
  def deleteAll(p: List[(String, Int, String)]): ZIO[Any, RepositoryError, Int]

object BankAccountRepository:

  def create(item: BankAccount):ZIO[BankAccountRepository, RepositoryError, Int]=
    ZIO.serviceWithZIO[BankAccountRepository](_.create(item))

  def create(models: List[BankAccount]): ZIO[BankAccountRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[BankAccountRepository](_.create(models))

  def modify(model: BankAccount): ZIO[BankAccountRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[BankAccountRepository](_.modify(model))

  def modify(models: List[BankAccount]): ZIO[BankAccountRepository, RepositoryError, Int]=
    ZIO.serviceWithZIO[BankAccountRepository](_.modify(models))
    
  def bankAccout4All(modelId:Int): ZIO[BankAccountRepository, RepositoryError, List[BankAccount]] =
    ZIO.serviceWithZIO[BankAccountRepository](_.bankAccout4All(modelId))
    
  def all(Id: (Int, String)): ZIO[BankAccountRepository, RepositoryError, List[BankAccount]] =
    ZIO.serviceWithZIO[BankAccountRepository](_.all(Id))

  def getById(Id: (String, Int, String)): ZIO[BankAccountRepository, RepositoryError, BankAccount]=
    ZIO.serviceWithZIO[BankAccountRepository](_.getById(Id))

  def getBy(ids: List[String], modelid: Int, company: String): ZIO[BankAccountRepository, RepositoryError, List[BankAccount]]=
    ZIO.serviceWithZIO[BankAccountRepository](_.getBy(ids, modelid, company))
  def delete(p: (String, Int, String)): ZIO[BankAccountRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[BankAccountRepository](_.delete(p))
    
  def deleteAll(p:List[(String, Int, String)]): ZIO[BankAccountRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[BankAccountRepository](_.deleteAll(p))