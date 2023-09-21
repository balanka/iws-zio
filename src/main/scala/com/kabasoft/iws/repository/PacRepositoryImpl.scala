package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.common.zeroAmount
import com.kabasoft.iws.domain.PeriodicAccountBalance
import zio._
import zio.prelude.FlipOps
import com.kabasoft.iws.repository.Schema.pacSchema
import zio.sql.ConnectionPool
import zio.stream._

final class PacRepositoryImpl(pool: ConnectionPool) extends PacRepository with IWSTableDescriptionPostgres {

  import AggregationDef._

  lazy val driverLayer = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))

  val pac = defineTable[PeriodicAccountBalance]("periodic_account_balance")

  val (id, account, period, idebit, icredit, debit, credit, currency, company, name, modelid) = pac.columns

  val SELECT = select(id, account, period, idebit, icredit, debit, credit, currency, company, name, modelid).from(pac)


  def whereClause(accountId: String, companyId: String, from: Int, to: Int) = {
    val list = if (accountId.contains('*')) {
      List(company === companyId, period >= from, period <= to)
    } else {
      List(account === accountId, company === companyId, period >= from, period <= to)
    }
    list.fold(Expr.literal(true))(_ && _)
  }

  private def whereClause(fromPeriod: Int, toPeriod: Int, companyId: String) =
    List(company === companyId, period >= fromPeriod, period <= toPeriod)
      .fold(Expr.literal(true))(_ && _)

  private def whereClause(idx: String, companyId: String) =
    List(id === idx, company === companyId)
      .fold(Expr.literal(true))(_ && _)

  def getBalancesQuery(fromPeriod: Int, toPeriod: Int, companyId: String) =
    select(
      (Max(id) as "id"),
      account,
      (Max(period) as "period"),
      (SumDec(idebit) as "idebit"),
      (SumDec(icredit) as "icredit"),
      (SumDec(debit) as "debit"),
      (SumDec(credit) as "credit"),
      currency,
      company,
      name,
      modelid)
      .from(pac)
      .where(whereClause(fromPeriod, toPeriod, companyId))
      .groupBy(account, currency, company, name, modelid)
      .orderBy(account.descending)



  override def create(models_ : List[PeriodicAccountBalance]): ZIO[Any, RepositoryError, Int] =
    if (models_.isEmpty) {
      ZIO.succeed(0)
    } else {
      val query = insertInto(pac)(id, account, period, idebit, icredit, debit, credit, currency, company, name, modelid).values(models_.map(toTuple))
      val insertSql = {
        renderInsert(query)
      }
      ZIO.logInfo(s"Query to insert PeriodicAccountBalance is ${insertSql}") *>
        execute(query).provideAndLog(driverLayer)
    }

  def modify(model: PeriodicAccountBalance): ZIO[Any, RepositoryError, Int] = {
    val update_ = build(model)
    execute(update_)
      .provideLayer(driverLayer)
      .mapError(e => RepositoryError(e.getMessage))
  }

  private def build(model: PeriodicAccountBalance) =
      update(pac)
        .set(idebit, model.idebit)
        .set(debit, model.debit)
        .set(icredit, model.icredit)
        .set(credit, model.credit)
        .where(whereClause(model.id, model.company))


  def modify(models: List[PeriodicAccountBalance]): ZIO[Any, RepositoryError, Int] =
    if (models.isEmpty) {
      ZIO.logInfo(s"Trying to update an empty List  update PeriodicAccountBalance is <<<<<<<<<<<<<}") *>
        ZIO.succeed(0)
    } else {
      val update_ = models.map(build)
      ZIO.logInfo(s"Trying to update an empty List  update PeriodicAccountBalance ${update_.map(renderUpdate)}") *>
      executeBatchUpdate(update_)
        .provideLayer(driverLayer)
        .mapBoth(e => RepositoryError(e.getMessage), _.sum)
    }

  override def list(companyId: String): ZStream[Any, RepositoryError, PeriodicAccountBalance] = {
    val selectAll = SELECT.where(company === companyId)
    ZStream.fromZIO(
      ZIO.logDebug(s"Query to execute findAll is ${renderRead(selectAll)}")
    ) *> execute(selectAll.to((PeriodicAccountBalance.apply _).tupled))
        .provideDriver(driverLayer)
  }

  override def all(companyId: String): ZIO[Any, RepositoryError, List[PeriodicAccountBalance]] =
    list(companyId).runCollect.map(_.toList)

  override def getBy(id: String, companyId: String): ZIO[Any, RepositoryError, PeriodicAccountBalance] = {
    val selectAll = SELECT.where(whereClause(id, companyId))

    ZIO.logDebug(s"Query to execute findBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to((PeriodicAccountBalance.apply _).tupled))
        .findFirst(driverLayer, id, PeriodicAccountBalance.dummy)
  }

  override def getByIds(ids: List[String], companyId: String): ZIO[Any, RepositoryError, List[PeriodicAccountBalance]] =
    ids.map(id =>getBy(id, companyId)).flip

  override def getByModelId(modelId: Int, companyId: String): ZIO[Any, RepositoryError, PeriodicAccountBalance] = {
    val selectAll = SELECT.where((modelid === modelId) && (company === companyId))

    ZIO.logDebug(s"Query to execute getByModelId is ${renderRead(selectAll)}") *>
      execute(selectAll.to((PeriodicAccountBalance.apply _).tupled))
        .findFirstInt(driverLayer, modelId)
  }

  def findBalance4Period(
                          fromPeriod: Int,
                          toPeriod: Int,
                          companyId: String
                        ): ZStream[Any, RepositoryError, PeriodicAccountBalance] = {
    val selectAll = getBalancesQuery(fromPeriod, toPeriod, companyId)
    ZStream.fromZIO(
      ZIO.logDebug(s"Query to execute findBalance4Period is ${renderRead(selectAll)}")
    ) *>
      execute(selectAll.to(x => PeriodicAccountBalance.applyX(x)))
        .provideDriver(driverLayer)
  }

  def getBalances4Period(fromPeriod: Int, toPeriod: Int, companyId: String): ZStream[Any, RepositoryError, PeriodicAccountBalance] = {
    val query = getBalancesQuery(fromPeriod, toPeriod, companyId)
    ZStream.fromZIO(
      ZIO.logDebug(s"Query to execute getBalances4Period is ${renderRead(query)}")
    ) *>
      execute(query.to((PeriodicAccountBalance.apply _).tupled))
        .provideDriver(driverLayer)
  }

  def find4Period(fromPeriod: Int, toPeriod: Int, companyId: String): ZStream[Any, RepositoryError, PeriodicAccountBalance] = {
    val selectAll = SELECT
      .where(whereClause(fromPeriod, toPeriod, companyId))
      .orderBy(account.descending)

    ZStream.fromZIO(
      ZIO.logDebug(s"Query to execute find4Period is ${renderRead(selectAll)}")
    ) *> execute(selectAll.to((PeriodicAccountBalance.apply _).tupled))
      .provideDriver(driverLayer)
  }

  def find4Period(accountId: String, fromPeriod: Int, toPeriod: Int, companyId: String): ZStream[Any, RepositoryError, PeriodicAccountBalance] = {
    val selectAll = SELECT
      .where(whereClause(accountId, companyId, fromPeriod, toPeriod ))
      .orderBy(account.descending)

    ZStream.fromZIO(
      ZIO.logDebug(s"Query to execute find4Period is ${renderRead(selectAll)}")
    ) *> execute(selectAll.to((PeriodicAccountBalance.apply _).tupled))
      .filter(e=>e.cbalance.compareTo(zeroAmount) !=0 || e.dbalance.compareTo(zeroAmount)!=0)
      .provideDriver(driverLayer)
  }

  def toTuple(pac: PeriodicAccountBalance) =
    (pac.id, pac.account, pac.period, pac.idebit, pac.icredit, pac.debit, pac.credit, pac.currency, pac.company, pac.name, pac.modelid)
}

object PacRepositoryImpl {

  val live: ZLayer[ConnectionPool, RepositoryError, PacRepository] =
    ZLayer.fromFunction(new PacRepositoryImpl(_))
}
