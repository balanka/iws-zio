package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.SalaryItem
import zio._
import zio.stream._

trait SalaryItemRepository:
  def create(item: SalaryItem, flag: Boolean):ZIO[Any, RepositoryError, Int]
  def create(models: List[SalaryItem]):ZIO[Any, RepositoryError, Int]
  def modify(model: SalaryItem):ZIO[Any, RepositoryError, Int]
  def modify(models: List[SalaryItem]):ZIO[Any, RepositoryError, Int]
  def all(Id: (Int, String)): ZIO[Any, RepositoryError, List[SalaryItem]]
  def getById(Id: (String, Int, String)):ZIO[Any, RepositoryError, SalaryItem]
  def getBy(ids: List[String], modelid: Int, company: String):ZIO[Any, RepositoryError, List[SalaryItem]]
  def delete(p: (String, Int, String)):ZIO[Any, RepositoryError, Int]


object SalaryItemRepository:
  def create(item: SalaryItem, flag: Boolean):ZIO[SalaryItemRepository, RepositoryError, Int]=
    ZIO.serviceWithZIO[SalaryItemRepository](_.create(item, flag))
  def create(models: List[SalaryItem]): ZIO[SalaryItemRepository, RepositoryError,Int] =
    ZIO.serviceWithZIO[SalaryItemRepository](_.create(models))
  def modify(model: SalaryItem): ZIO[SalaryItemRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[SalaryItemRepository](_.modify(model))
  def modify(models: List[SalaryItem]): ZIO[SalaryItemRepository, RepositoryError, Int]=
    ZIO.serviceWithZIO[SalaryItemRepository](_.modify(models))
  def all(Id: (Int, String)): ZIO[SalaryItemRepository, RepositoryError, List[SalaryItem]] =
    ZIO.serviceWithZIO[SalaryItemRepository](_.all(Id))
  def getById(Id: (String, Int, String)): ZIO[SalaryItemRepository, RepositoryError, SalaryItem]=
    ZIO.serviceWithZIO[SalaryItemRepository](_.getById(Id))
  def getBy(ids: List[String], modelid: Int, company: String): ZIO[SalaryItemRepository, RepositoryError, List[SalaryItem]]=
    ZIO.serviceWithZIO[SalaryItemRepository](_.getBy(ids, modelid, company))
  def delete(p: (String, Int, String)): ZIO[SalaryItemRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[SalaryItemRepository](_.delete(p))

