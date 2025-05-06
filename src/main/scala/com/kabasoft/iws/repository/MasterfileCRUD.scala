package com.kabasoft.iws.repository

import cats.*
import cats.syntax.all.*
import cats.effect.Resource
import com.kabasoft.iws.domain.AppError.RepositoryError
import skunk._
import zio._
import zio.interop.catz._
import skunk.data.Completion

trait MasterfileCRUD:
  def tryExec[A](xa: Transaction[Task], pc: PreparedCommand[Task, A], models: List[A]): Task[Unit] =

    for
      sp <- xa.savepoint
      _ <- exec(pc, models)
          .handleErrorWith(ex =>
            ZIO.logInfo(s"Unique violation: ${ex.getMessage}, rolling back...") *>
              xa.rollback(sp))
    yield ()
  
  def tryExec [A, B](xa: Transaction[Task], pciCustomer: PreparedCommand[Task, A]
                      , pciBankAcc: PreparedCommand[Task, B]
                      , customers: List[A], bankaccounts:List[B]): Task[Unit] =

    for
      sp <- xa.savepoint
      _ <- exec(pciCustomer, customers) *>
        exec(pciBankAcc, bankaccounts)
          .handleErrorWith(ex =>
            ZIO.logInfo(s"Unique violation: ${ex.getMessage}, rolling back...") *>
              xa.rollback(sp))
    yield ()

  // C= Customer.TYPE3
  // D= BankAccount.TYPE2
  def tryExec[A, B, C, D](xa: Transaction[Task], pciCustomer: PreparedCommand[Task, A]
              , pciBankAcc: PreparedCommand[Task, B]
              , pcuCustomer: PreparedCommand[Task, C]
              , pcuBankAcc: PreparedCommand[Task, D]
              , customers: List[A], newBankaccounts: List[B] 
              , oldCustomers: List[C], oldBankaccounts: List[D]): Task[Unit] =

    for
      sp <- xa.savepoint
      _ <- exec(pciCustomer, customers) *>
        exec(pciBankAcc, newBankaccounts) *>
        exec(pcuCustomer, oldCustomers) *>
        exec(pcuBankAcc, oldBankaccounts)
          .handleErrorWith(ex =>
            ZIO.logInfo(s"Unique violation: ${ex.getMessage}, rolling back...") *>
              xa.rollback(sp))
    yield ()

  def tryExec[A, B, C, D, E](xa: Transaction[Task]
                             , pciCustomer: PreparedCommand[Task, A]
                             , pciBankAcc: PreparedCommand[Task, B]
                             , pcuCustomer: PreparedCommand[Task, C]
                             , pcuBankAcc: PreparedCommand[Task, D]
                             , pcdBankAcc: PreparedCommand[Task, E]
                             , customers: List[A]
                             , newBankaccounts: List[B]
                             , oldCustomers: List[C]
                             , oldBankaccounts: List[D]
                             , bankacc2Delete: List[E]): Task[Unit] =

    for
      sp <- xa.savepoint
      _ <- exec(pciCustomer, customers).debug("ZZZZZZZZ0>>>") *>
        exec(pciBankAcc, newBankaccounts).debug("ZZZZZZZZ1>>>") *>
        exec(pcuCustomer, oldCustomers).debug("ZZZZZZZZ2>>>") *>
        exec(pcuBankAcc, oldBankaccounts).debug("ZZZZZZZZ3>>>") *>
        exec(pcdBankAcc, bankacc2Delete).debug("ZZZZZZZZ4>>>")
          .handleErrorWith(ex =>
            ZIO.logInfo(s"Unique violation: ${ex.getMessage}, rolling back...") *>
              xa.rollback(sp))
    yield ()
    
  def tryExec[A, B, C, D, E, F](xa: Transaction[Task]
                                , pciCustomer: PreparedCommand[Task, A]
                                , pciBankAcc: PreparedCommand[Task, B]
                                , pcuCustomer: PreparedCommand[Task, C]
                                , pcuBankAcc: PreparedCommand[Task, D]
                                , pcdCustomer: PreparedCommand[Task, E]
                                , pcdBankAcc: PreparedCommand[Task, F]
                                , customers: List[A]
                                , newBankaccounts: List[B]
                                , oldCustomers: List[C]
                                , oldBankaccounts: List[D]
                                , customer2Delete: List[E]
                                , bankacc2Delete: List[F]): Task[Unit] =

    for
      sp <- xa.savepoint
      _ <- exec(pciCustomer, customers) *>
        exec(pciBankAcc, newBankaccounts) *>
        exec(pcuCustomer, oldCustomers) *>
        exec(pcuBankAcc, oldBankaccounts) *>
        exec(pcdCustomer, customer2Delete) *>
        exec(pcdBankAcc, bankacc2Delete)
          .handleErrorWith(ex =>
            ZIO.logInfo(s"Unique violation: ${ex.getMessage}, rolling back...") *>
              xa.rollback(sp))
    yield ()

  def tryExec2[A, B, C, D](xa: Transaction[Task], pciPac: PreparedCommand[Task, A]
                           , pciJour: PreparedCommand[Task, B]
                           , pcuPac: PreparedCommand[Task, C]
                           , pcuFtr: PreparedCommand[Task, D]
                           , pac2Insert: List[A], journals: List[B]
                           , pac2update: List[C], models: List[D]): Task[Unit] =

    for
      sp <- xa.savepoint
      _ <- exec(pciPac, pac2Insert) *>
        exec(pciJour, journals) *>
        exec(pcuPac, pac2update) *>
        exec(pcuFtr, models)
          .handleErrorWith(ex =>
            ZIO.logInfo(s"Unique violation: ${ex.getMessage}, rolling back...") *>
              xa.rollback(sp))
    yield ()

  def tryExec3[A, B, C, D, E, F, G, H](xa: Transaction[Task], pciPac: PreparedCommand[Task, A]
                          , pciStock: PreparedCommand[Task, B]
                          , pciJour: PreparedCommand[Task, C]
                          , pciTransLog: PreparedCommand[Task, D]
                          , pcuPac: PreparedCommand[Task, E]
                          , pcuStock: PreparedCommand[Task, F]
                          , pcuArt: PreparedCommand[Task, G]
                          , pcuFtr: PreparedCommand[Task, H]
                          , newPac: List[A]
                          , newStock: List[B]
                          , journals: List[C]
                          , transLogEntries: List[D]
                          , pac2update: List[E]
                          , stock2update: List[F]
                          , articles: List[G]
                          , models:List[H]): Task[Unit] =

    for
      sp <- xa.savepoint
      _ <- exec(pciPac, newPac) *>
        exec(pciStock, newStock) *>
        exec(pciJour, journals) *>
        exec(pciTransLog, transLogEntries) *>
        exec(pcuPac, pac2update) *>
        exec(pcuStock, stock2update) *>
        exec(pcuArt, articles) *>
        exec(pcuFtr, models)
          .handleErrorWith(ex =>
            ZIO.logInfo(s"Unique violation: ${ex.getMessage}, rolling back...") *>
              xa.rollback(sp))
    yield () 
  
  def queryWithTx[A, B](postgres: Resource[Task, Session[Task]], p: A, q: Query[A, B]):ZIO[Any, RepositoryError, List[B]] =
    postgres
      .use: session =>
        session
          .prepare(q)
          .flatMap: ps =>
            ps.stream(p, 1024).compile.toList.recoverWith:
             case SqlState.SyntaxError(ex) => ZIO.logInfo(s"Unique violation: ${ex.constraintName.getOrElse("<unknown>")}, rolling back...")*>
               ZIO.succeed(List.empty[B])
             case _ =>
               ZIO.logInfo(s"Error:  rolling back...")*>
               ZIO.succeed(List.empty[B])
      .mapBoth(e => RepositoryError(e.getMessage), list => list)//.debug(" ALLL Called ")
    
  def queryWithTxUnique[A, B](postgres: Resource[Task, Session[Task]], p:A, q:Query[A, B]):ZIO[Any, RepositoryError, B] =
     postgres
       .use: session =>
         session
          .prepare(q)
          .flatMap(ps => ps.unique(p))
       .mapBoth(e => RepositoryError(e.getMessage), a => a)//.debug("Data/Error")

  def queryWithTxUnique[ A](postgres: Resource[Task, Session[Task]],  q: Query[Void, A]): ZIO[Any, RepositoryError, A] =
    postgres.use: session =>
                session.unique(q)
            .mapBoth(e => RepositoryError(e.getMessage), a => a)//.debug("Data/Error")
  
  def executeWithTx[A](postgres: Resource[Task, Session[Task]], p: A, comd: Command[A], size: Int): ZIO[Any, RepositoryError, Int] =
    postgres
      .use: session =>
        session.transaction.use: xa =>
          session
            .prepare(comd)
            .flatMap: cmd =>
              xa.savepoint
              cmd.execute(p).debug("vcbvcbvcbvcbvcbvcvb").recoverWith:
                case SqlState.UniqueViolation(ex) =>
                  ZIO.logInfo(s"Unique violation: ${ex.constraintName.getOrElse("<unknown>")}, rolling back...") *>
                    xa.rollback
                case ex =>
                  ZIO.logInfo(s"Error: ${ex.getMessage} rolling back...!!!!") *>
                    xa.rollback
      .mapBoth(e => RepositoryError(e.getMessage), _ => size)
    
  def executeWithTx(postgres: Resource[Task, Session[Task]],  cmd: Command[Void], size: Int): ZIO[Any, RepositoryError, Int] =
    postgres
      .use: session =>
        session.transaction.use: xa =>
          session
            .execute(cmd).debug("ffffffffffffffff")
            .recoverWith:
                case SqlState.UniqueViolation(ex) =>
                  ZIO.logInfo(s"Unique violation: ${ex.constraintName.getOrElse("<unknown>")}, rolling back...") *>
                    xa.rollback
                case ex =>
                  ZIO.logInfo(s"Error: ${ex.getMessage} rolling back...!!!!") *>
                    xa.rollback
      .mapBoth(e => RepositoryError(e.getMessage), _ => size)
  
  def executeWithTx[A](session: Session[Task], p: A, comd: Command[A], size: Int): Task[Int] =
    session.transaction.use: xa =>
      session
        .prepare(comd)
        .flatMap: cmd =>
          xa.savepoint
          cmd.execute(p).recoverWith:
            case SqlState.UniqueViolation(ex) =>
              ZIO.logInfo(s"Unique violation: ${ex.constraintName.getOrElse("<unknown>")}, rolling back...") *>
                xa.rollback
            case _ =>
              ZIO.logInfo(s"Error:  rolling back...") *>
                xa.rollback
    .mapBoth(e => e, _ => size)

  def executeWithTx[A](xa: Transaction[Task], command: PreparedCommand[Task, A], p:A): Task[Int] =
    xa.savepoint
    command.execute(p)
      .recoverWith {
        case SqlState.UniqueViolation(ex) =>
          ZIO.logInfo(s"Unique violation: ${ex.constraintName.getOrElse("<unknown>")}, rolling back...") *>
            xa.rollback
        case _ =>
          ZIO.logInfo(s"Error:  rolling back...") *>
            xa.rollback
      }
      .mapBoth(e => e, _ => 1)
 
  def executeWithTx[A, B](xa: Transaction[Task]
                          , command: PreparedCommand[Task, List[B]]
                          , p: List[A]
                          , encoder: A => B): Task[Int] =
    xa.savepoint
      command.execute(p.map(encoder)).debug("Executing")
      .recoverWith {
        case SqlState.UniqueViolation(ex) =>
          ZIO.logInfo(s"Unique violation: ${ex.constraintName.getOrElse("<unknown>")}, rolling back...") *>
            xa.rollback
        case _ =>
          ZIO.logInfo(s"Error:  rolling back...") *>
            xa.rollback
      }
      .mapBoth(e => e, _ => p.size)  

  
  def executeWithTx[A, B](postgres: Resource[Task, Session[Task]], p: A, encoder: A => B, comd: Command[B], size: Int): ZIO[Any, RepositoryError, Int] =
    for {
      _ <- ZIO.logInfo(s"Executing: $comd with param ${encoder(p)}")
      result <- postgres
        .use: session =>
          session.transaction.use: xa =>
            session
              .prepare(comd).debug("SSSSSSSSSSSS")
              .flatMap: cmd =>
                xa.savepoint
                cmd.execute(encoder(p)).debug("vcbccvcvcvcvvc").recoverWith:
                  case SqlState.UniqueViolation(ex) =>
                    ZIO.logInfo(s"Unique violation: ${ex.constraintName.getOrElse("<unknown>")}, rolling back...") *>
                      xa.rollback
                  case _ =>
                    ZIO.logInfo(s"Error:  rolling back...") *>
                      xa.rollback
        .mapBoth(e => RepositoryError(e.getMessage), _ => size)            
    } yield result
  
  def exec[T](pc: PreparedCommand[Task, T], list: List[T]): Task[Unit] =
    list.traverse_ { p =>
      for
        //transid <- sql"SELECT NEXTVAL('master_compta_id_seq')".query[Long].unique
        _ <- pc.execute(p).debug("RRRRRRRR>>>")
      yield ()
    }
  
  def executeBatchWithTxK[A, B](postgres: Resource[Task, Session[Task]], params: List[A], cmdx: Command[B], encode: A => B): ZIO[Any, RepositoryError, Int] = for {
     u <- postgres
       .use: session =>
         session.transaction.use: xa =>
           session
             .prepare(cmdx)
             .flatMap: cmd =>
               xa.savepoint
               params.traverse(p =>
                 cmd.execute(encode(p)).recoverWith {
                   case SqlState.UniqueViolation(ex) =>
                     ZIO.logInfo(s"Unique violation: ${ex.constraintName.getOrElse("<unknown>")}, rolling back...") *>
                       xa.rollback
                   case _ =>
                     ZIO.logInfo(s"Error:  rolling back...") *>
                       xa.rollback
                 })
       .mapBoth(e => RepositoryError(e.getMessage), _ => params.size).as(params.size)
   } yield u








