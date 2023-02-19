package com.kabasoft.iws.repository

import com.kabasoft.iws.repository.Schema.companySchema
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Company
import zio._
import zio.stream._
import zio.sql.ConnectionPool

final class CompanyRepositoryImpl(pool: ConnectionPool) extends CompanyRepository with PostgresTableDescription {

  lazy val driverLayer = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))

  val company = defineTable[Company]("company")

  val (
    id,
    name,
    street,
    zip,
    city,
    state,
    country,
    email,
    partner,
    phone,
    bankAcc,
    iban,
    taxCode,
    vatCode,
    currency,
    locale,
    balanceSheetAcc,
    incomeStmtAcc,
    modelid
  ) = company.columns

  val SELECT                                                                 = select(
    id,
    name,
    street,
    zip,
    city,
    state,
    country,
    email,
    partner,
    phone,
    bankAcc,
    iban,
    taxCode,
    vatCode,
    currency,
    locale,
    balanceSheetAcc,
    incomeStmtAcc,
    modelid
  )
    .from(company)
  def toTuple(c: Company)                                                    = (
    c.id,
    c.name,
    c.street,
    c.zip,
    c.city,
    c.state,
    c.country,
    c.email,
    c.partner,
    c.phone,
    c.bankAcc,
    c.iban,
    c.taxCode,
    c.vatCode,
    c.currency,
    c.locale,
    c.balanceSheetAcc,
    c.incomeStmtAcc,
    c.modelid
  )
//
  override def create(c: Company): ZIO[Any, RepositoryError, Unit]           = {
    val query = insertInto(company)(
      id,
      name,
      street,
      zip,
      city,
      state,
      country,
      email,
      partner,
      phone,
      bankAcc,
      iban,
      taxCode,
      vatCode,
      currency,
      locale,
      balanceSheetAcc,
      incomeStmtAcc,
      modelid
    ).values(toTuple(c))

    ZIO.logDebug(s"Query to insert Company is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
        .unit
  }
  override def create(models: List[Company]): ZIO[Any, RepositoryError, Int] = {
    val data  = models.map(toTuple(_))
    val query = insertInto(company)(
      id,
      name,
      street,
      zip,
      city,
      state,
      country,
      email,
      partner,
      phone,
      bankAcc,
      iban,
      taxCode,
      vatCode,
      currency,
      locale,
      balanceSheetAcc,
      incomeStmtAcc,
      modelid
    ).values(data)

    ZIO.logDebug(s"Query to insert Company is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
  }
  override def delete(item: String): ZIO[Any, RepositoryError, Int]          =
    execute(deleteFrom(company).where((id === item)))
      .provideLayer(driverLayer)
      .mapError(e => RepositoryError(e.getCause()))

  override def modify(model: Company): ZIO[Any, RepositoryError, Int] = {
    val update_ = update(company)
      .set(name, model.name)
      .where((id === model.id))
    ZIO.logDebug(s"Query Update Company is ${renderUpdate(update_)}") *>
      execute(update_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getCause()))
  }

  override def all: ZIO[Any, RepositoryError, List[Company]] =
    list.runCollect.map(_.toList)
  /* for {
    companies <- list.runCollect.map(_.toList)
    bankAccounts_ <- listBankAccount(companyId).runCollect.map(_.toList)
  } yield companies.map(c => c.copy(bankaccounts = bankAccounts_.filter(_.owner == c.id)))

   */

  override def list: ZStream[Any, RepositoryError, Company]          = {
    val selectAll =
      select(
        id,
        name,
        street,
        zip,
        city,
        state,
        country,
        email,
        partner,
        phone,
        bankAcc,
        iban,
        taxCode,
        vatCode,
        currency,
        locale,
        balanceSheetAcc,
        incomeStmtAcc,
        modelid
      ).from(company) // .where(id === companyId)

    /* ZStream.fromZIO(
      ZIO.logInfo(s"Query to execute findAll is ${renderRead(selectAll)}")
    ) *>
     */
    execute(selectAll.to((Company.apply _).tupled))
      .provideDriver(driverLayer)
  }
  override def getBy(Id: String): ZIO[Any, RepositoryError, Company] = {
    val selectAll =
      select(
        id,
        name,
        street,
        zip,
        city,
        state,
        country,
        email,
        partner,
        phone,
        bankAcc,
        iban,
        taxCode,
        vatCode,
        currency,
        locale,
        balanceSheetAcc,
        incomeStmtAcc,
        modelid
      ).from(company) // .where(id === Id)

    // ZIO.logDebug(s"Query to execute findBy is ${renderRead(selectAll)}") *>
    execute(selectAll.to((Company.apply _).tupled))
      // execute(selectAll.to[Company](c => Company(c)))
      .findFirst(driverLayer, Id)
  }

}

object CompanyRepositoryImpl {
  val live: ZLayer[ConnectionPool, Throwable, CompanyRepository] =
    ZLayer.fromFunction(new CompanyRepositoryImpl(_))
}
