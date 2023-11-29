package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Fmodule
import com.kabasoft.iws.repository.Schema.fmoduleSchema
import zio._
import zio.sql.ConnectionPool
import zio.stream._

final class FModuleRepositoryImpl(pool: ConnectionPool) extends FModuleRepository with IWSTableDescriptionPostgres {

  lazy val driverLayer = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))

  val fmodule = defineTable[Fmodule]("fmodule")

  val (id, name, description, enterdate, changedate, postingdate, account, is_debit, modelid, company) = fmodule.columns

  val SELECT                                                                           = select(id, name, description, enterdate, changedate, postingdate, account, is_debit, modelid, company).from(fmodule)


  def whereClause(Id: Int, companyId: String) =
    List(id === Id, company === companyId)
      .fold(Expr.literal(true))(_ && _)

  def whereClause(Ids: List[Int], companyId: String) =
    List(company === companyId, id in Ids).fold(Expr.literal(true))(_ && _)
  override def create(c: Fmodule): ZIO[Any, RepositoryError, Fmodule]                        = create2(c)*>getBy((c.id, c.company))

  override def create(models: List[Fmodule]): ZIO[Any, RepositoryError, List[Fmodule]]              =
    if(models.isEmpty){
      ZIO.succeed(List.empty[Fmodule])
    }else {
      create2(models)*>getBy(models.map(_.id), models.head.company)
    }

  override def create2(c: Fmodule): ZIO[Any, RepositoryError, Unit]                        = {
    val query = insertInto(fmodule)(id, name, description, enterdate, changedate, postingdate, account, is_debit, modelid, company).values(Fmodule.unapply(c).get)

    ZIO.logDebug(s"Query to insert Bank is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
        .unit
  }
  override def create2(models: List[Fmodule]): ZIO[Any, RepositoryError, Int]              = {
    val data  = models.map(Fmodule.unapply(_).get)
    val query = insertInto(fmodule)(id, name, description, enterdate, changedate, postingdate, account, is_debit, modelid, company).values(data)

    ZIO.logDebug(s"Query to insert Bank is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
  }
  override def delete(idx: Int, companyId: String): ZIO[Any, RepositoryError, Int] = {
    val delete_ = deleteFrom(fmodule).where((company === companyId) && (id === idx) )
    ZIO.logDebug(s"Delete Bank is ${renderDelete(delete_)}") *>
      execute(delete_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getMessage))
  }

  override def modify(model: Fmodule): ZIO[Any, RepositoryError, Int] = {
    val update_ = update(fmodule)
      .set(name, model.name)
      .set(description, model.description)
      .where(whereClause( model.id,  model.company))
    ZIO.logDebug(s"Query Update Bank is ${renderUpdate(update_)}") *>
      execute(update_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getMessage))
  }

  override def all(companyId: String): ZIO[Any, RepositoryError, List[Fmodule]] =
    list(companyId).runCollect.map(_.toList)

  override def list(companyId: String): ZStream[Any, RepositoryError, Fmodule]                   = {
    val selectAll = SELECT.where(company === companyId)
    ZStream.fromZIO(
      ZIO.logDebug(s"Query to execute findAll is ${renderRead(selectAll)}")
    ) *>
      execute(selectAll.to((Fmodule.apply _).tupled))
        .provideDriver(driverLayer)
  }
  override def getBy(Id:(Int,String)): ZIO[Any, RepositoryError, Fmodule]          = {
    val selectAll = SELECT.where(whereClause (Id._1, Id._2))

    ZIO.logDebug(s"Query to execute findBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to((Fmodule.apply _).tupled))
        .findFirstInt(driverLayer, Id._1)
  }

  def getBy(ids: List[Int], company: String): ZIO[Any, RepositoryError, List[Fmodule]] = for {
    banks <- getBy_(ids, company).runCollect.map(_.toList)
  } yield banks

  def getBy_(ids: List[Int], company: String): ZStream[Any, RepositoryError, Fmodule] = {
    val selectAll = SELECT.where(whereClause(ids, company))
    execute(selectAll.to((Fmodule.apply _).tupled))
      .provideDriver(driverLayer)
  }
  override def getByModelId(id: (Int, String)): ZIO[Any, RepositoryError, List[Fmodule]] = for {
    all <- getByModelIdStream(id._1, id._2).runCollect.map(_.toList)
  } yield all

  override def getByModelIdStream(modelId: Int, companyId: String): ZStream[Any, RepositoryError, Fmodule] = {
    val selectAll = SELECT.where((modelid === modelId) && (company === companyId))
    ZStream.fromZIO(ZIO.logDebug(s"Query to execute getByModelId is ${renderRead(selectAll)}")) *>
      execute(selectAll.to((Fmodule.apply _).tupled))
        .provideDriver(driverLayer)
  }

}

object FModuleRepositoryImpl {
  val live: ZLayer[ConnectionPool, Throwable, FModuleRepository] =
    ZLayer.fromFunction(new FModuleRepositoryImpl(_))
}
