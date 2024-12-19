package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Article
import zio.*

trait ArticleRepository:
  def create(item: Article): ZIO[Any, RepositoryError, Int]
  def create(models: List[Article]): ZIO[Any, RepositoryError, Int]
  def modify(model: Article): ZIO[Any, RepositoryError, Int]
  def modify(models: List[Article]): ZIO[Any, RepositoryError, Int]
  def all(Id: (Int, String)): ZIO[Any, RepositoryError, List[Article]]
  def getById(Id: (String, Int, String)): ZIO[Any, RepositoryError, Article]
  def getBy(ids: List[String], modelid: Int, company: String): ZIO[Any, RepositoryError, List[Article]]
  def delete(p: (String, Int, String)): ZIO[Any, RepositoryError, Int]

object ArticleRepository:

  def create(item: Article):ZIO[ArticleRepository, RepositoryError, Int]=
    ZIO.serviceWithZIO[ArticleRepository](_.create(item))

  def create(models: List[Article]): ZIO[ArticleRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[ArticleRepository](_.create(models))

  def modify(model: Article): ZIO[ArticleRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[ArticleRepository](_.modify(model))

  def modify(models: List[Article]): ZIO[ArticleRepository, RepositoryError, Int]=
    ZIO.serviceWithZIO[ArticleRepository](_.modify(models))

  def all(Id: (Int, String)): ZIO[ArticleRepository, RepositoryError, List[Article]] =
    ZIO.serviceWithZIO[ArticleRepository](_.all(Id))

  def getById(Id: (String, Int, String)): ZIO[ArticleRepository, RepositoryError, Article]=
    ZIO.serviceWithZIO[ArticleRepository](_.getById(Id))

  def getBy(ids: List[String], modelid: Int, company: String): ZIO[ArticleRepository, RepositoryError, List[Article]]=
    ZIO.serviceWithZIO[ArticleRepository](_.getBy(ids, modelid, company))

  def delete(p: (String, Int, String)): ZIO[ArticleRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[ArticleRepository](_.delete(p))