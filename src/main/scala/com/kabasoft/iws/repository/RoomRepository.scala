package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Room
import zio._

trait RoomRepository:
  def create(item: Room):ZIO[Any, RepositoryError, Int]
  def create(models: List[Room]):ZIO[Any, RepositoryError, Int]
  def modify(model: Room):ZIO[Any, RepositoryError, Int]
  def modify(models: List[Room]): ZIO[Any, RepositoryError, Int]
  def all(Id: (Int, String)):ZIO[Any, RepositoryError, List[Room]]
  def getById(Id: (String, Int, String)):ZIO[Any, RepositoryError, Room]
  def getBy(ids: List[String], modelid: Int, company: String):ZIO[Any, RepositoryError, List[Room]]
  def getByParent(parent: String, modelid: Int, company: String): ZIO[Any, RepositoryError, List[Room]]
  def delete(p: (String, Int, String)):ZIO[Any, RepositoryError, Int]

object RoomRepository:
  def create(item: Room): ZIO[RoomRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[RoomRepository](_.create(item))
  def create(models: List[Room]): ZIO[RoomRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[RoomRepository](_.create(models))
  def modify(model: Room): ZIO[RoomRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[RoomRepository](_.modify(model))
  def modify(models: List[Room]): ZIO[RoomRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[RoomRepository](_.modify(models))
  def all(Id: (Int, String)): ZIO[RoomRepository, RepositoryError, List[Room]] =
    ZIO.serviceWithZIO[RoomRepository](_.all(Id))
  def getById(Id: (String, Int, String)): ZIO[RoomRepository, RepositoryError, Room] =
    ZIO.serviceWithZIO[RoomRepository](_.getById(Id))
  def getBy(ids: List[String], modelid: Int, company: String): ZIO[RoomRepository, RepositoryError, List[Room]] =
    ZIO.serviceWithZIO[RoomRepository](_.getBy(ids, modelid, company))
  def getByParent(parent: String, modelid: Int, company: String): ZIO[RoomRepository, RepositoryError, List[Room]]=
   ZIO.serviceWithZIO[RoomRepository](_.getByParent(parent, modelid, company))
  def delete(p: (String, Int, String)): ZIO[RoomRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[RoomRepository](_.delete(p))

