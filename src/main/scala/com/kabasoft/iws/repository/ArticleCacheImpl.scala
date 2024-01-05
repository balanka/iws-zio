package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.Article
import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._
import zio.cache.{Cache, Lookup}

import java.util.concurrent.TimeUnit


final case class ArticleCacheImpl (repository: ArticleRepository) extends ArticleCache  {

  override def all(Id:(Int,  String)): ZIO[Any, RepositoryError, List[Article]] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup(repository.all)).flatMap(_.get(Id))

  override def getBy(id:(String, String)): ZIO[Any, RepositoryError, Article] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup[(String,String),Any,RepositoryError, Article](repository.getBy)).flatMap(_.get(id))

  override def getByModelId(id: (Int, String)): ZIO[Any, RepositoryError, List[Article]] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup[(Int, String), Any, RepositoryError, List[Article]](repository.getByModelId)).flatMap(_.get(id))
}

object ArticleCacheImpl {
  val live: ZLayer[ArticleRepository , RepositoryError, ArticleCache] =
    ZLayer.fromFunction(new ArticleCacheImpl(_))
}
