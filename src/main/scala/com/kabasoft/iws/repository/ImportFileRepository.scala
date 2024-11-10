package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.ImportFile
import zio.Task
import zio.*
import zio.stream.*

import java.time.LocalDate

trait ImportFileRepository:

  def create(item: ImportFile, flag: Boolean): ZIO[Any, RepositoryError, Int]

  def create(models: List[ImportFile]):ZIO[Any, RepositoryError, Int]

  def modify(model: ImportFile):ZIO[Any, RepositoryError, Int]

  def modify(models: List[ImportFile]):ZIO[Any, RepositoryError, Int]

  def all(Id: (Int, String)):ZIO[Any, RepositoryError, List[ImportFile]]
  
  def getById(Id: (String, Int, String)):ZIO[Any, RepositoryError, ImportFile]
  
  def getBy(ids: List[String], modelid: Int, company: String):ZIO[Any, RepositoryError, List[ImportFile]]

  def delete(p: (String, Int, String)):ZIO[Any, RepositoryError, Int]

object ImportFileRepository:

  def create(item: ImportFile, flag: Boolean):ZIO[ImportFileRepository, RepositoryError, Int]=
    ZIO.serviceWithZIO[ImportFileRepository](_.create(item, flag))

  def create(models: List[ImportFile]): ZIO[ImportFileRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[ImportFileRepository](_.create(models))

  def modify(model: ImportFile): ZIO[ImportFileRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[ImportFileRepository](_.modify(model))

  def modify(models: List[ImportFile]): ZIO[ImportFileRepository, RepositoryError, Int]=
    ZIO.serviceWithZIO[ImportFileRepository](_.modify(models))

  def all(Id: (Int, String)): ZIO[ImportFileRepository, RepositoryError, List[ImportFile]] =
    ZIO.serviceWithZIO[ImportFileRepository](_.all(Id))

  def getById(Id: (String, Int, String)): ZIO[ImportFileRepository, RepositoryError, ImportFile]=
    ZIO.serviceWithZIO[ImportFileRepository](_.getById(Id))

  def getBy(ids: List[String], modelid: Int, company: String): ZIO[ImportFileRepository, RepositoryError, List[ImportFile]]=
    ZIO.serviceWithZIO[ImportFileRepository](_.getBy(ids, modelid, company))

  def delete(p: (String, Int, String)): ZIO[ImportFileRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[ImportFileRepository](_.delete(p))