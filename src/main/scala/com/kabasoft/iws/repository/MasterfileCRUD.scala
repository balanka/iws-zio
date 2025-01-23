package com.kabasoft.iws.repository

import cats.*
import cats.syntax.all.*
import cats.effect.{IO, Resource}
import com.kabasoft.iws.domain.AppError.RepositoryError
import skunk.*
import zio.*
import zio.interop.catz.*
import cats.syntax.applicativeError.catsSyntaxApplicativeError
import com.kabasoft.iws.domain.{Article, TransactionLog}
import com.kabasoft.iws.repository.MasterfileCRUD.{ExecCommand, InsertBatch, UpdateCommand}
import skunk.data.Completion
import zio.stream.ZStream
import zio.stream.interop.fs2z.*

object MasterfileCRUD:
  final case class UpdateCommand[A, B](param: A, encoder: A => B, cmd: Command[B])
  final case class ExecCommand[A, B](param: List[A], encoder: A => B, cmd: Command[B])
  final case class InsertBatch[A, B](param: List[A], encoder: A => B, cmd: Command[List[B]])
  
trait MasterfileCRUD:
  def tryExec [A, B](xa: Transaction[Task], pciCustomer: PreparedCommand[Task, A]
                      , pciBankAcc: PreparedCommand[Task, B]
                      , customers: List[A], bankaccounts:List[B]): Task[Unit] =

    for
      _ <- ZIO.logInfo(s"Trying to insert $customers")
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
      _ <- ZIO.logInfo(s"Trying to insert $customers")
      _ <- ZIO.logInfo(s"Trying to uodate $oldCustomers")
      sp <- xa.savepoint
      _ <- exec(pciCustomer, customers) *>
        exec(pciBankAcc, newBankaccounts) *>
        exec(pcuCustomer, oldCustomers) *>
        exec(pcuBankAcc, oldBankaccounts)
          .handleErrorWith(ex =>
            ZIO.logInfo(s"Unique violation: ${ex.getMessage}, rolling back...") *>
              xa.rollback(sp))
    yield ()

  def tryExec[A, B, C, D, E, F](xa: Transaction[Task], pciCustomer: PreparedCommand[Task, A]
                          , pciBankAcc: PreparedCommand[Task, B]
                          , pcuCustomer: PreparedCommand[Task, C]
                          , pcuBankAcc: PreparedCommand[Task, D]
                          , pcdCustomer: PreparedCommand[Task, E]
                          , pcdBankAcc: PreparedCommand[Task, F]
                          , customers: List[A], newBankaccounts: List[B]
                          , oldCustomers: List[C], oldBankaccounts: List[D]
                          , customer2Delete: List[E], bankacc2Delete: List[F] ): Task[Unit] =

    for
      _ <- ZIO.logInfo(s"Trying to insert $customers")
      _ <- ZIO.logInfo(s"Trying to uodate $oldCustomers")
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
      _ <- ZIO.logInfo(s"Trying to insert PAC $pac2Insert")
      _ <- ZIO.logInfo(s"Trying to update  PAC$pac2update")
      _ <- ZIO.logInfo(s"Trying to insert Journal $journals")
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
      _ <- ZIO.logInfo(s"Trying to insert PAC $newPac")
      _ <- ZIO.logInfo(s"Trying to update  PAC$pac2update")
      _ <- ZIO.logInfo(s"Trying to insert Journal $journals")
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
      .mapBoth(e => RepositoryError(e.getMessage), list => list)
    
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
              cmd.execute(p).recoverWith:
                case SqlState.UniqueViolation(ex) =>
                  ZIO.logInfo(s"Unique violation: ${ex.constraintName.getOrElse("<unknown>")}, rolling back...") *>
                    xa.rollback
                case _ =>
                  ZIO.logInfo(s"Error:  rolling back...") *>
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
      command.execute(p.map(encoder))
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
    ZIO.logInfo(s"Executing: $comd with param ${encoder(p)}") *>
      postgres
        .use: session =>
          session.transaction.use: xa =>
            session
              .prepare(comd)
              .flatMap: cmd =>
                xa.savepoint
                cmd.execute(encoder(p)).recoverWith:
                  case SqlState.UniqueViolation(ex) =>
                    ZIO.logInfo(s"Unique violation: ${ex.constraintName.getOrElse("<unknown>")}, rolling back...") *>
                      xa.rollback
                  case _ =>
                    ZIO.logInfo(s"Error:  rolling back...") *>
                      xa.rollback
        .mapBoth(e => RepositoryError(e.getMessage), _ => size)

  def executeWithTxW[A, B](session: Session[Task], p: A, encoder:A=>B, comd: Command[B], size: Int): ZIO[Any, RepositoryError, Int] =
    ZIO.logInfo(s"Executing: $comd with param ${encoder(p)}") *>
    // postgres
      // .use: session =>
          session.transaction.use: xa =>
            session
             .prepare(comd)
             .flatMap: cmd =>
               xa.savepoint
                cmd.execute(encoder(p)).recoverWith:
                  case SqlState.UniqueViolation(ex) =>
                    ZIO.logInfo(s"Unique violation: ${ex.constraintName.getOrElse("<unknown>")}, rolling back...") *>
                    xa.rollback
                  case _ =>
                    ZIO.logInfo(s"Error:  rolling back...") *>
                    xa.rollback
         .mapBoth(e => RepositoryError(e.getMessage), _ => size)

  def exec1[A, B](pc: PreparedCommand[Task, B], list: List[A], encoder:A=>B): Task[Unit] =
    list.map(encoder).traverse_ { p =>
      for
        _ <- ZIO.logInfo(s"Trying to run an insert/update/delete command $p")
        _ <- pc.execute(p)
      yield ()
    }
  
  def exec[T](pc: PreparedCommand[Task, T], list: List[T]): Task[Unit] =
    list.traverse_ { p =>
      for
        _ <-  ZIO.logInfo(s"Trying to run an insert/update/delete command $p")
        _ <- pc.execute(p)
      yield ()
    }

  def executeBatchWithTx[A, B, C, D](postgres: Resource[Task, Session[Task]]
                                    , commands: List[UpdateCommand[A,B]]
                                    , commandLPs: List[InsertBatch[C, D]]): Unit =
    postgres
      .use: session =>
        session.transaction.use: xa =>
          commands.traverse(command =>
            session
              .prepare(command.cmd)
              .flatMap: cmd =>
                 xa.savepoint
                 cmd.execute(command.encoder(command.param))).*>
          commandLPs.traverse(command =>
                session
                  .prepare(command.cmd)
                  .map: cmd =>
                     //exec1(cmd, command.param, command.encoder)).*>
                      xa.savepoint
                      cmd.execute(command.param.map(command.encoder)))
            .recoverWith:
             case SqlState.UniqueViolation(ex) =>
                 ZIO.logInfo(s"Unique violation: ${ex.constraintName.getOrElse("<unknown>")}, rolling back...") *>
                  xa.rollback
             case _ =>
                 ZIO.logInfo(s"Error:  rolling back...") *>
                 xa.rollback
  

  def executeWithTxR[A, B](xa: Transaction[Task], p: List[A], encoder: A => B
                           , command: PreparedCommand[Task, List[B]], size: Int): Task[Int] =
    xa.savepoint
    command.execute(p.map(encoder)).recoverWith {
        case SqlState.UniqueViolation(ex) =>
          ZIO.logInfo(s"Unique violation: ${ex.constraintName.getOrElse("<unknown>")}, rolling back...") *>
            xa.rollback
        case _ =>
          ZIO.logInfo(s"Error:  rolling back...") *>
            xa.rollback
      }
      .mapBoth(e => e, _ => size)
    
  type TYPE [A, B] = (PreparedCommand[Task, List[B]], List[A],  A => B)
  
  def executeWithoutTx[A, B]( command: PreparedCommand[Task, List[B]],  p: List[A], encoder: A => B): Task[Int] =
          command.execute(p.map(encoder)).mapBoth(e => e, _ => p.size)

  def executeWithTxa[A](session: Session[Task], xa: Transaction[Task], p: A, command: Command[A]): Task[Int]=
    session
      .prepare(command)
      .flatMap: cmd =>
         executeWithTx(xa, cmd, p)

  def executeWithTxR[A, B](session: Session[Task], xa:Transaction[Task], p: List[A], encoder: A => B, command: Command[List[B]]): Task[Int] =
         session
           .prepare(command)
            .flatMap: cmd =>
              executeWithTx(xa, cmd, p, encoder)


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

def execPreparedCommand[A, B](postgres: Resource[Task, Session[Task]], params:List[A], encoder:A=>B, commands: List[Command[B]]): ZIO[Any, RepositoryError, Int] = for {
  u <- postgres
    .use: session =>
      session.transaction.use: xa =>
        commands.traverse(cmdx =>
          session
          .prepare(cmdx)
          .flatMap: cmd =>
           params.traverse(param =>
             cmd.execute(encoder(param)).recoverWith {
                case SqlState.UniqueViolation(ex) =>
                  ZIO.logInfo(s"Unique violation: ${ex.constraintName.getOrElse("<unknown>")}, rolling back...") *>
                    xa.rollback
                case _ =>
                  ZIO.logInfo(s"Error:  rolling back...") *>
                    xa.rollback
              })
        )
    .mapBoth(e => RepositoryError(e.getMessage), list => commands.size) //.as(params.size)
} yield u

def executeBatchWithTZ[A, B, C, D](session: Session[Task]
                                   , commands: List[InsertBatch[A, B]]
                                   , commandLPs: List[InsertBatch[C, D]]): Task[Int] =
      session.transaction.use: xa =>
        (commands.traverse(command =>
          session
            .prepare(command.cmd)
            .flatMap: cmd =>
              xa.savepoint
              cmd.execute(command.param.map(command.encoder)).debug(" Executing InsertBatch A"))*>
          commandLPs.traverse(command =>
            session
              .prepare(command.cmd)
              .flatMap: cmd =>
                xa.savepoint
                cmd.execute(command.param.map(command.encoder)).debug(" Executing InsertBatch B"))).recoverWith:
          case SqlState.UniqueViolation(ex) =>
            ZIO.logInfo(s"Unique violation: ${ex.constraintName.getOrElse("<unknown>")}, rolling back...") *> xa.rollback
          case _ =>
            ZIO.logInfo(s"Error:  rolling back...") *> xa.rollback
    .mapBoth(e => e, _ => commands.map(x=>x.param).size + commandLPs.map(x=>x.param).size)        

  def xy[C, F](session: Session[Task], deleteCommands: List[ExecCommand[C, F]]) =
      deleteCommands.traverse: command =>
        session
         .prepare(command.cmd)
         .flatMap: cmd =>
            command.param.traverse: p =>
               cmd.execute(command.encoder(p))

  def xx[A, B](session: Session[Task], insertCommands: List[InsertBatch[A, B]]) =
       insertCommands.traverse: command =>
         session
          .prepare(command.cmd)
          .flatMap: cmd =>
           //xa.savepoint
             cmd.execute(command.param.map(command.encoder))
  def xx2[C, D](session: Session[Task], insertCommands: List[InsertBatch[C, D]]) =
        insertCommands.traverse: command =>
          session
            .prepare(command.cmd)
            .flatMap: cmd =>
            //xa.savepoint
               cmd.execute(command.param.map(command.encoder))
  def ax[A, B](session: Session[Task], updateCommands: List[UpdateCommand[A, B]]) =
       //session.transaction.use: xa =>
         updateCommands.traverse: command =>
           session
             .prepare(command.cmd)
             .flatMap: cmd =>
             //xa.savepoint
               cmd.execute(command.encoder(command.param))

  def ax2[C, E](session: Session[Task], commandLPs: List[ExecCommand[C, E]]) =
        commandLPs.traverse: command =>
           session
             .prepare(command.cmd)
             .flatMap: cmd =>
             //xa.savepoint
                command.param.traverse: p =>
                   cmd.execute(command.encoder(p))
  def executeBatchWithTx2[A, B, C, D, E, F](postgres: Resource[Task, Session[Task]]
                                          , commands: List[UpdateCommand[A, B]]
                                          , insertCommands: List[InsertBatch[A, B]]
                                          , deleteCommands: List[ExecCommand[C, F]]
                                          , insertCommands2: List[InsertBatch[C, D]]
                                          , commandLPs: List[ExecCommand[C, E]],
                                         ): Unit = {
    postgres
      .use:
      session =>
        session.transaction.use: xa =>
          ax(session, commands)
          .*>(xy(session, deleteCommands))
          .*>(xx(session, insertCommands))
          .*>(xx2(session, insertCommands2))
          .*>(ax2(session, commandLPs))
          .recoverWith:
             case SqlState.UniqueViolation(ex)=>
               ZIO.logInfo(s"Unique violation: ${ex.constraintName.getOrElse("<unknown>")}, rolling back...") *>
               xa.rollback
             case _ =>
                  ZIO.logInfo(s"Error:  rolling back...") *>
                  xa.rollback
}