package com.kabasoft.iws.repository

import zio._
import zio.stream._
import zio.sql.ConnectionPool
import com.kabasoft.iws.domain._
import com.kabasoft.iws.domain.AppError._

import java.util.UUID

final class CustomerRepositoryImpl(
  pool: ConnectionPool
) extends CustomerRepository
    with PostgresTableDescription {

  lazy val driverLayer = ZLayer
    .make[SqlDriver](
      SqlDriver.live,
      ZLayer.succeed(pool)
    )

  override def findAll(): ZStream[Any, RepositoryError, Customer] = {
    val selectAll =
      select(customerId ++ fName ++ lName ++ verified ++ dob).from(customers)

    ZStream.fromZIO(
      ZIO.logInfo(s"Query to execute findAll is ${renderRead(selectAll)}")
    ) *>
      execute(selectAll.to((Customer.apply _).tupled))
        .provideDriver(driverLayer)
  }

  override def findById(id: UUID): ZIO[Any, RepositoryError, Customer] = {
    val selectAll = select(customerId ++ fName ++ lName ++ verified ++ dob)
      .from(customers)
      .where(customerId === id)

    ZIO.logInfo(s"Query to execute findById is ${renderRead(selectAll)}") *>
      execute(selectAll.to((Customer.apply _).tupled))
        .findFirst(driverLayer, id)
  }

  override def add(customer: Customer): ZIO[Any, RepositoryError, Unit] = {
    val query =
      insertInto(customers)(customerId ++ dob ++ fName ++ lName ++ verified)
        .values(
          (
            customer.id,
            customer.dateOfBirth,
            customer.fname,
            customer.lname,
            customer.verified
          )
        )

    ZIO.logInfo(s"Query to insert customer is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
        .unit
  }

  override def add(customer: List[Customer]): ZIO[Any, RepositoryError, Int] = {
    val data =
      customer.map(c => (c.id, c.dateOfBirth, c.fname, c.lname, c.verified))

    val query =
      insertInto(customers)(customerId ++ dob ++ fName ++ lName ++ verified)
        .values(data)

    ZIO.logInfo(s"Query to insert customers is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
  }

  override def removeAll(): ZIO[Any, RepositoryError, Int] =
    execute(deleteFrom(customers))
      .provideLayer(driverLayer)
      .mapError(e => RepositoryError(e.getCause()))

  override def modify(model: Customer): ZIO[Any, RepositoryError, Int] = {
    val update_ = update(customers)
      .set(fName, model.fname)
      .set(lName, model.lname)
      .where((customerId === model.id))
    ZIO.logInfo(s"Query Update Customer is ${renderUpdate(update_)}") *>
      execute(update_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getCause()))
  }
}

object CustomerRepositoryImpl {
  val live: ZLayer[ConnectionPool, Throwable, CustomerRepository] =
    ZLayer.fromFunction(new CustomerRepositoryImpl(_))
}
