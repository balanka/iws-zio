package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Masterfile
import zio.Task
import zio.*
import zio.stream.*

import java.time.LocalDate

trait MasterfileRepository:
  def create(item: Masterfile, flag: Boolean): ZIO[Any, RepositoryError, Int]
  def create(models: List[Masterfile]): ZIO[Any, RepositoryError, Int]
  def modify(model: Masterfile): ZIO[Any, RepositoryError, Int]
  def modify(models: List[Masterfile]):ZIO[Any, RepositoryError, Int]
  def all(Id: (Int, String)): ZIO[Any, RepositoryError, List[Masterfile]]
  def getById(Id: (String, Int, String)): ZIO[Any, RepositoryError, Masterfile]
  def getBy(ids: List[String], modelid: Int, company: String): ZIO[Any, RepositoryError, List[Masterfile]]
  def delete(p: (String, Int, String)): ZIO[Any, RepositoryError, Int]

object MasterfileRepository:
  def create(item: Masterfile, flag: Boolean):ZIO[MasterfileRepository, RepositoryError, Int]=
    ZIO.serviceWithZIO[MasterfileRepository](_.create(item, flag).mapError(e => RepositoryError(e.message)))
  def create(models: List[Masterfile]): ZIO[MasterfileRepository, RepositoryError,Int] =
    ZIO.serviceWithZIO[MasterfileRepository](_.create(models).mapError(e => RepositoryError(e.message)))
  def modify(model: Masterfile): ZIO[MasterfileRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[MasterfileRepository](_.modify(model).mapError(e => RepositoryError(e.message)))
  def modify(models: List[Masterfile]): ZIO[MasterfileRepository, RepositoryError, Int]=
    ZIO.serviceWithZIO[MasterfileRepository](_.modify(models).mapError(e => RepositoryError(e.message)))
  def all(Id: (Int, String)): ZIO[MasterfileRepository, RepositoryError, List[Masterfile]] =
    ZIO.serviceWithZIO[MasterfileRepository](_.all(Id).mapError(e => RepositoryError(e.message)))
  def getById(Id: (String, Int, String)): ZIO[MasterfileRepository, RepositoryError, Masterfile]=
    ZIO.serviceWithZIO[MasterfileRepository](_.getById(Id).mapError(e => RepositoryError(e.message)))
  def getBy(ids: List[String], modelid: Int, company: String): ZIO[MasterfileRepository, RepositoryError, List[Masterfile]]=
    ZIO.serviceWithZIO[MasterfileRepository](_.getBy(ids, modelid, company).mapError(e => RepositoryError(e.message)))
  def delete(p: (String, Int, String)): ZIO[MasterfileRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[MasterfileRepository](_.delete(p).mapError(e => RepositoryError(e.message)))