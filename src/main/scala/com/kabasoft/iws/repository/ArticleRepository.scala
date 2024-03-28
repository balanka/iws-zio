package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.Article
import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._
import zio.stream._

trait ArticleRepository {
  def create(item: Article): ZIO[Any, RepositoryError, Article]

  def create(models: List[Article]): ZIO[Any, RepositoryError, List[Article]]
  def create2(item: Article): ZIO[Any, RepositoryError, Unit]
  def create2(models: List[Article]): ZIO[Any, RepositoryError, Int]
  def delete(item: String, company: String): ZIO[Any, RepositoryError, Int]
  def delete(items: List[String], company: String): ZIO[Any, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, company)))
  def list(Id:(Int,  String)): ZStream[Any, RepositoryError, Article]
  def all(Id:(Int,  String)): ZIO[Any, RepositoryError, List[Article]]

//  def getById(Ids:List[String]): ZIO[Any, RepositoryError, List[Article]]
//  def getById(Id:String): ZIO[Any, RepositoryError, Option[Article]]
  def getBy(id: (String,String)): ZIO[Any, RepositoryError, Article]
  def getBy(ids: List[String], company: String): ZIO[Any, RepositoryError, List[Article]]
  def getByModelId(id: (Int,  String)): ZIO[Any, RepositoryError, List[Article]]
  def getByModelIdStream(modelid: Int, company: String): ZStream[Any, RepositoryError, Article]
  def modify(model: Article): ZIO[Any, RepositoryError, Int]
  def modify(models: List[Article]): ZIO[Any, RepositoryError, Int]
}
object ArticleRepository {

  def create(item: Article): ZIO[ArticleRepository, RepositoryError, Article] =
    ZIO.service[ArticleRepository] flatMap (_.create(item))

  def create(items: List[Article]): ZIO[ArticleRepository, RepositoryError, List[Article]] =
    ZIO.service[ArticleRepository] flatMap (_.create(items))
  def create2(item: Article): ZIO[ArticleRepository, RepositoryError, Unit] =
    ZIO.service[ArticleRepository] flatMap (_.create2(item))

  def create2(items: List[Article]): ZIO[ArticleRepository, RepositoryError, Int] =
    ZIO.service[ArticleRepository] flatMap (_.create2(items))

  def delete(item: String, company: String): ZIO[ArticleRepository, RepositoryError, Int] =
    ZIO.service[ArticleRepository] flatMap (_.delete(item, company))

  def delete(items: List[String], company: String): ZIO[ArticleRepository, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, company)))

  def list(Id:(Int,  String)): ZStream[ArticleRepository, RepositoryError, Article] =
    ZStream.service[ArticleRepository] flatMap (_.list(Id))

  def all(Id:(Int,  String)): ZIO[ArticleRepository, RepositoryError, List[Article]] =
    ZIO.serviceWithZIO[ArticleRepository](_.all(Id))
  def getBy(id: (String,String)): ZIO[ArticleRepository, RepositoryError, Article] =
    ZIO.serviceWithZIO[ArticleRepository](_.getBy(id))

  def getBy(ids: List[String], companyId: String): ZIO[ArticleRepository, RepositoryError, List[Article]] =
    ZIO.serviceWithZIO[ArticleRepository](_.getBy(ids, companyId))
  def getByModelId(id: (Int,  String)): ZIO[ArticleRepository, RepositoryError, List[Article]] =
    ZIO.serviceWithZIO[ArticleRepository](_.getByModelId(id))
  def getByModelIdStream(modelid: Int, company: String): ZStream[ArticleRepository, RepositoryError, Article] =
    ZStream.service[ArticleRepository] flatMap (_.getByModelIdStream(modelid, company))

  def modify(model: Article): ZIO[ArticleRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[ArticleRepository](_.modify(model))

  def modify(models: List[Article]): ZIO[ArticleRepository, RepositoryError, Int] =
    ZIO.service[ArticleRepository] flatMap (_.modify(models))
}
