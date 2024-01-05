package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.Article
import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._

trait ArticleCache {
  def all(Id:(Int,  String)): ZIO[Any, RepositoryError, List[Article]]
  def getBy(id:(String,  String)): ZIO[Any, RepositoryError, Article]
  def getByModelId(id:(Int, String)): ZIO[Any, RepositoryError, List[Article]]

}
object ArticleCache {
  def all(Id:(Int,  String)): ZIO[ArticleCache, RepositoryError, List[Article]] =
    ZIO.service[ArticleCache] flatMap (_.all(Id))

  def getBy(id:(String, String)): ZIO[ArticleCache, RepositoryError, Article]=
    ZIO.service[ArticleCache] flatMap (_.getBy(id))

  def getByModelId(id:(Int, String)): ZIO[ArticleCache, RepositoryError, List[Article]] =
    ZIO.service[ArticleCache] flatMap (_.getByModelId(id))

}
