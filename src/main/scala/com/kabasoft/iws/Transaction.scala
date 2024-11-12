package com.kabasoft.iws
// Copyright (c) 2018-2020 by Rob Norris
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

import cats.effect._
import cats.implicits._
import natchez.Trace.Implicits.noop
import skunk._
import skunk.codec.all._
import skunk.implicits._

// a data type
case class Pet(name: String, age: Short)

// a service interface
trait PetService[F[_]] {
  def tryInsertAll(pets: List[Pet]): F[Unit]
  def selectAll: F[List[Pet]]
}

// a companion with a constructor
object PetService {

  // command to insert a pet
  private val insertOne: Command[Pet] =
    sql"INSERT INTO pets VALUES ($varchar, $int2)"
      .command
      .to[Pet]
    
  private val updateOne: Command[(Short, String)] =
    sql"UPDATE pets set age=$int2  WHERE name=$varchar"
      .command
      
  // query to select all pets
  private val all: Query[Void, Pet] =
    sql"SELECT name, age FROM pets"
      .query(varchar *: int2)
      .to[Pet]

  // construct a PetService, preparing our statement once on construction
  def fromSession(s: Session[IO]): IO[PetService[IO]] =
    s.prepare(insertOne).map { pc =>
      new PetService[IO] {

        // Attempt to insert all pets, in a single transaction, handling each in turn and rolling
        // back to a savepoint if a unique violation is encountered. Note that a bulk insert with an
        // ON CONFLICT clause would be much more efficient, this is just for demonstration.
        def tryInsertAll(pets: List[Pet]): IO[Unit] =
          s.transaction.use { xa =>
            pets.foldMapA { p =>
              for {
                _  <- IO.println(s"Trying to insert $p")
                sp <- xa.savepoint
                res  <- pc.execute(p).recoverWith {
                  case SqlState.UniqueViolation(ex) =>
                    IO.println(s"Unique violation: ${ex.constraintName.getOrElse("<unknown>")}, rolling back...") *>
                      xa.rollback(sp)
                }.as(1)
              } yield res
            }
          }

        def selectAll: IO[List[Pet]] = s.execute(all)
      }
    }

}

object TransactionExample extends IOApp {

  // a source of sessions
  val session: Resource[IO, Session[IO]] =
    Session.single(
      host     = "0.0.0.0",
      user     = "postgres",
      database = "iws",
      password = Some("iws123")
    )

  // a resource that creates and drops a temporary table
  def withPetsTable(s: Session[IO]): Resource[IO, Unit] = {
    val alloc = s.execute(sql"CREATE TEMP TABLE pets (name varchar unique, age int2)".command).void
    val free  = s.execute(sql"DROP TABLE pets".command).void
    Resource.make(alloc)(_ => free)
  }

  // a resource that creates and drops a temporary table
//  def withPetUpdate(s: Session[IO], age:Short, name:String): Resource[IO, List[Pet]] = {
//    val pet = s.execute(sql"UPDATE pets set age=$int2  WHERE name=$varchar".command)
//    val all = s.execute(sql"select version();".query(text)).void
//    Resource.make(pet)((12,"Bob") => ())
//  }

  // We can monitor the changing transaction status by tapping into the provided `fs2.Signal`
  def withTransactionStatusLogger[A](ss: Session[IO]): Resource[IO, Unit] = {
    val alloc: IO[Fiber[IO, Throwable, Unit]] =
      ss.transactionStatus
        .discrete
        .changes
        .evalMap(s => IO.println(s"xa status: $s"))
        .compile
        .drain
        .start
    Resource.make(alloc)(_.cancel).void
  }

  // A resource that puts it all together.
  val resource: Resource[IO, PetService[IO]] =
    for {
      s  <- session
      _  <- withPetsTable(s)
      _  <- withTransactionStatusLogger(s)
      ps <- Resource.eval(PetService.fromSession(s))
    } yield ps

  // Some test data
  val pets = List(
    Pet("Alice", 3),
    Pet("Bob",  42),
    Pet("Bob",  21),
    Pet("Steve", 9)
  )

  // Our entry point
  def run(args: List[String]): IO[ExitCode] =
    resource.use { ps =>
      for {
        _   <- ps.tryInsertAll(pets)
        //_   <- ps.updateOne()
        all <- ps.selectAll
        _   <- all.traverse_(p => IO.println(p))
      } yield ExitCode.Success
    }

}