package com.kabasoft.iws.repository
import com.kabasoft.iws.repository.Schema.{payrollTaxRangeSchema, repositoryErrorSchema}
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.PayrollTaxRange
import zio._
import zio.sql.ConnectionPool
import zio.stream._

final class PayrollTaxRangeRepositoryImpl(pool: ConnectionPool) extends PayrollTaxRangeRepository with IWSTableDescriptionPostgres {

  lazy val driverLayer = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))

  val payrollTaxRange = defineTable[PayrollTaxRange]("payroll_tax_range")

  val (id, from_amount, to_amount, tax, tax_class, modelid, company) = payrollTaxRange.columns
  val SELECT = select(id, from_amount, to_amount, tax, tax_class, modelid, company).from(payrollTaxRange)

  def whereClause(Id: String, modelId:Int, companyId: String) =
    List(id === Id, modelid === modelId, company === companyId)
      .fold(Expr.literal(true))(_ && _)

  def whereClause(Ids: List[String], modelId:Int, companyId: String) =
    List(company === companyId, modelid === modelId, id in Ids).fold(Expr.literal(true))(_ && _)

  override def create(c: PayrollTaxRange): ZIO[Any, RepositoryError, PayrollTaxRange] = create2(c) *> getBy((c.id, c.modelid, c.company))

  override def create(models: List[PayrollTaxRange]): ZIO[Any, RepositoryError, List[PayrollTaxRange]] =
    if (models.isEmpty) {
      ZIO.succeed(List.empty[PayrollTaxRange])
    } else {
      create2(models) *> getBy(models.map(_.id), models.head.modelid, models.head.company)
    }
  override def create2(c: PayrollTaxRange): ZIO[Any, RepositoryError, Unit]                         = {
    val query = insertInto(payrollTaxRange)(id, from_amount, to_amount, tax, tax_class, modelid, company).values(PayrollTaxRange.unapply(c).get)

    ZIO.logDebug(s"Query to insert payrollTaxRange is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
        .unit
  }
  override def create2(models: List[PayrollTaxRange]): ZIO[Any, RepositoryError, Int]               = {
    val data  = models.map(PayrollTaxRange.unapply(_).get)
    val query = insertInto(payrollTaxRange)(id, from_amount, to_amount, tax, tax_class, modelid, company).values(data)

    ZIO.logDebug(s"Query to insert payrollTaxRange is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
  }
  override def delete(idx: String, modelId:Int, companyId: String): ZIO[Any, RepositoryError, Int]    =
    execute(deleteFrom(payrollTaxRange).where((company === companyId) && (id === idx) && (modelid === modelId)))
      .provideLayer(driverLayer)
      .mapError(e => RepositoryError(e.getMessage))

  override def modify(model: PayrollTaxRange): ZIO[Any, RepositoryError, Int] = {
    val update_ = update(payrollTaxRange)
      .set(from_amount, model.fromAmount)
      .set(to_amount, model.toAmount)
      .set(tax, model.tax)
      .set(tax_class, model.taxClass)
      .where(whereClause(model.id,  model.modelid, model.company))
    ZIO.logDebug(s"Query Update payrollTaxRange is ${renderUpdate(update_)}") *>
      execute(update_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getMessage))
  }

  override def all(Id:(Int, String)): ZIO[Any, RepositoryError, List[PayrollTaxRange]]                  =
    list(Id).runCollect.map(_.toList)

  override def list(Id:(Int, String)): ZStream[Any, RepositoryError, PayrollTaxRange]                   = {
    val selectAll = SELECT.where(modelid === Id._1 && company === Id._2)
    ZStream.fromZIO(
      ZIO.logDebug(s"Query to execute findAll is ${renderRead(selectAll)}")
    ) *>
      execute(selectAll.to((PayrollTaxRange.apply _).tupled))
        .provideDriver(driverLayer)
  }
  override def getBy(Id:(String,  Int, String)): ZIO[Any, RepositoryError, PayrollTaxRange]          = {
    val selectAll = SELECT.where(whereClause(Id._1, Id._2, Id._3))

    ZIO.logDebug(s"Query to execute findBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to((PayrollTaxRange.apply _).tupled))
        .findFirst(driverLayer, Id._1)
  }

  def getBy(ids: List[String], modelId:Int, company: String): ZIO[Any, RepositoryError, List[PayrollTaxRange]] = for {
    vats <- getBy_(ids, modelId, company).runCollect.map(_.toList)
  } yield vats

  def getBy_(ids: List[String], modelId:Int, company: String): ZStream[Any, RepositoryError, PayrollTaxRange] = {
    val selectAll = SELECT.where(whereClause(ids, modelId, company))
    execute(selectAll.to((PayrollTaxRange.apply _).tupled))
      .provideDriver(driverLayer)
  }
  override def getByModelId(id: (Int, String)): ZIO[Any, RepositoryError, List[PayrollTaxRange]] = for {
    all <- getByModelIdStream(id._1, id._2).runCollect.map(_.toList)
  } yield all

  override def getByModelIdStream(modelId: Int, companyId: String): ZStream[Any, RepositoryError, PayrollTaxRange] = {
    val selectAll = SELECT.where((modelid === modelId) && (company === companyId))
    ZStream.fromZIO(ZIO.logDebug(s"Query to execute getByModelId is ${renderRead(selectAll)}")) *>
      execute(selectAll.to((PayrollTaxRange.apply _).tupled))
        .provideDriver(driverLayer)
  }

}

object PayrollTaxRangeRepositoryImpl {
  val live: ZLayer[ConnectionPool, Throwable, PayrollTaxRangeRepository] =
    ZLayer.fromFunction(new PayrollTaxRangeRepositoryImpl(_))
}
