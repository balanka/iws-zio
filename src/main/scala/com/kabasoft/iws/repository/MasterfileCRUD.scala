package com.kabasoft.iws.repository

import cats.*
import cats.syntax.all.*
import cats.effect.Resource
import com.kabasoft.iws.domain.AppError.RepositoryError
import skunk.*
import zio.*
import zio.interop.catz.*
import cats.syntax.applicativeError.catsSyntaxApplicativeError
import com.kabasoft.iws.repository.MasterfileCRUD.{IwsCommand, InsertBatch, IwsCommandLP2}
import skunk.data.Completion
import zio.stream.ZStream
import zio.stream.interop.fs2z.*

object MasterfileCRUD:
  final case class IwsCommand[A, B](param: A, encoder: A => B, cmd: Command[B])
  final case class IwsCommandLP2[A, B](param: List[A], encoder: A => B, cmd: Command[B])
  final case class InsertBatch[A, B](param: List[A], encoder: A => B, cmd: Command[List[B]])
  
trait MasterfileCRUD:

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

  def queryWithTxS[A, B](postgres: Resource[Task, Session[Task]], p:A, q:Query[A, B]):ZIO[Any, RepositoryError, List[B]] =
    postgres
      .use: session =>
        session
          .prepare(q)
          .flatMap:
            ps => ps.stream(p, 1014).compile.toList
      .mapBoth(e => RepositoryError(e.getMessage), a => a).debug("Data/Error")

  def queryWithTxUnique[A, B](postgres: Resource[Task, Session[Task]], p:A, q:Query[A, B]):ZIO[Any, RepositoryError, B] =
     postgres
       .use: session =>
         session
          .prepare(q)
          .flatMap(ps => ps.unique(p))
       .mapBoth(e => RepositoryError(e.getMessage), a => a).debug("Data/Error")


  def executeWithTx_[A, B](postgres: Resource[Task, Session[Task]], iwsCmd:IwsCommand[A, B], size: Int): ZIO[Any, RepositoryError, Int] =
     postgres
       .use: session =>
         session.transaction.use: xa =>
           session
            .prepare(iwsCmd.cmd)
            .flatMap: cmd =>
               xa.savepoint
               cmd.execute(iwsCmd.encoder(iwsCmd.param)).recoverWith:
                 case SqlState.UniqueViolation(ex) =>
                   ZIO.logInfo(s"Unique violation: ${ex.constraintName.getOrElse("<unknown>")}, rolling back...") *>
                   xa.rollback
                 case _ =>
                   ZIO.logInfo(s"Error:  rolling back...") *>
                   xa.rollback
       .mapBoth(e => RepositoryError(e.getMessage), _ => size)

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
    
  def executeWithTx[A, B](postgres: Resource[Task, Session[Task]], p: A, encoder:A=>B, comd: Command[B], size: Int): ZIO[Any, RepositoryError, Int] =
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

//   def executeWithTx[A](postgres: Resource[Task, Session[Task]], list: List[A], comd: Command[A], size: Int): ZIO[Any, RepositoryError, Int] =
//     postgres
//       .use: session =>
//         session.transaction.use: xa =>
//           session
//           .prepare(comd)
//           .flatMap: cmd =>
//             xa.savepoint
//             cmd.execute(p).recoverWith:
//               case SqlState.UniqueViolation(ex) =>
//                 ZIO.logInfo(s"Unique violation: ${ex.constraintName.getOrElse("<unknown>")}, rolling back...") *>
//                   xa.rollback
//               case _ =>
//                 ZIO.logInfo(s"Error:  rolling back...") *>
//                   xa.rollback
//       .mapBoth(e => RepositoryError(e.getMessage), _ => size)

  def executeWithTx_[A, B](postgres: Resource[Task, Session[Task]], commands: List[IwsCommand[A, B]]) =
     postgres
       .use: session =>
         session.transaction.use: xa =>
           commands.traverse(command =>
             session
               .prepare(command.cmd)
               .flatMap: cmd =>
                 xa.savepoint
                 cmd.execute(command.encoder(command.param)).recoverWith {
                   case SqlState.UniqueViolation(ex) =>
                     ZIO.logInfo(s"Unique violation: ${ex.constraintName.getOrElse("<unknown>")}, rolling back...") *>
                       xa.rollback
                   case _ =>
                     ZIO.logInfo(s"Error:  rolling back...") *>
                       xa.rollback
                 }
           )
       .mapBoth(e => RepositoryError(e.getMessage), _ => commands.size)

  def executeBatchWithTx2[A, B, C, D, E, F](postgres: Resource[Task, Session[Task]]
                                     , commands: List[IwsCommand[A, B]]
                                     , deleteCommands: List[IwsCommandLP2[C, F]]
                                     //, insertCommands1: List[InsertBatch[A, B]]
                                     , insertCommands2: List[InsertBatch[C, D]]
                                     , commandLPs: List[IwsCommandLP2[C, E]],
                                     ): Unit =
    postgres
      .use: session =>
        session.transaction.use: xa =>
          commands.traverse(command =>
            session
              .prepare(command.cmd)
              .flatMap: cmd =>
                 xa.savepoint
                 cmd.execute(command.encoder(command.param))).*>
          deleteCommands.traverse(command =>
              session
                .prepare(command.cmd)
                .flatMap: cmd =>
                  xa.savepoint
                  command.param.traverse(p =>
                     cmd.execute(command.encoder(p)))).*>
          insertCommands2.traverse(command =>
                session
                  .prepare(command.cmd)
                  .flatMap: cmd =>
                    xa.savepoint
                    cmd.execute(command.param.map(command.encoder))).*>
          commandLPs.traverse(command => 
                session
                .prepare(command.cmd)
                .flatMap: cmd =>
                  xa.savepoint
                  command.param.traverse(p =>
                    cmd.execute(command.encoder(p))))
            .recoverWith:
              case SqlState.UniqueViolation(ex) =>
                ZIO.logInfo(s"Unique violation: ${ex.constraintName.getOrElse("<unknown>")}, rolling back...") *>
                  xa.rollback
              case _ =>
                ZIO.logInfo(s"Error:  rolling back...") *>
                  xa.rollback
                
  def executeBatchWithTx[A, B, C, D](postgres: Resource[Task, Session[Task]]
                                    , commands: List[IwsCommand[A,B]]
                                    , commandLPs: List[InsertBatch[C, D]]): Unit =
    postgres
      .use: session =>
        session.transaction.use: xa =>
          commands.traverse(command =>
            session
              .prepare(command.cmd)
              .flatMap: cmd =>
                xa.savepoint
                cmd.execute(command.encoder(command.param))
          ).*>(
            commandLPs.traverse(command =>
                session
                  .prepare(command.cmd)
                  .flatMap: cmd =>
                    xa.savepoint
                     cmd.execute(command.param.map(command.encoder)))
          )
            .recoverWith:
             case SqlState.UniqueViolation(ex) =>
                 ZIO.logInfo(s"Unique violation: ${ex.constraintName.getOrElse("<unknown>")}, rolling back...") *>
                  xa.rollback
             case _ =>
                 ZIO.logInfo(s"Error:  rolling back...") *>
                 xa.rollback



  def executeWithTxLP[A, B](postgres: Resource[Task, Session[Task]], command: InsertBatch[A, B]) =
     postgres
       .use: session =>
         session.transaction.use: xa =>
             session
               .prepare(command.cmd)
               .flatMap: cmd =>
                 //xa.savepoint
                 cmd.execute(command.param.map(command.encoder)).recoverWith {
                   case SqlState.UniqueViolation(ex) =>
                     ZIO.logInfo(s"Unique violation: ${ex.constraintName.getOrElse("<unknown>")}, rolling back...") *>
                       xa.rollback
                   case _ =>
                     ZIO.logInfo(s"Error:  rolling back...") *>
                       xa.rollback
                 }
          // )
       .mapBoth(e => RepositoryError(e.getMessage), _ => command.param.size)

  def executeWithTx[A](postgres: Resource[Task, Session[Task]], p: A, commands: List[Command[A]], size: Int): ZIO[Any, RepositoryError, Int] =
     postgres
       .use: session =>
         session.transaction.use: xa =>
           commands.traverse(command =>
             session
               .prepare(command)
               .flatMap: cmd =>
                 xa.savepoint
                 cmd.execute(p).recoverWith {
                   case SqlState.UniqueViolation(ex) =>
                     ZIO.logInfo(s"Unique violation: ${ex.constraintName.getOrElse("<unknown>")}, rolling back...") *>
                       xa.rollback
                   case _ =>
                     ZIO.logInfo(s"Error:  rolling back...") *>
                       xa.rollback
                 }
           )
       .mapBoth(e => RepositoryError(e.getMessage), _ => size)

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

   def executeBatchWithTx_[A, B](postgres: Resource[Task, Session[Task]],  commands: List[IwsCommand[A, B]]): ZIO[Any, RepositoryError, Int] = for {
    u <- postgres
      .use: session =>
        session.transaction.use: xa =>
          commands.traverse(iws =>
           session
            .prepare(iws.cmd)
            .flatMap: cmd =>
              xa.savepoint
                cmd.execute(iws.encoder(iws.param)).recoverWith {
                  case SqlState.UniqueViolation(ex) =>
                    ZIO.logInfo(s"Unique violation: ${ex.constraintName.getOrElse("<unknown>")}, rolling back...") *>
                      xa.rollback
                  case _ =>
                    ZIO.logInfo(s"Error:  rolling back...") *>
                      xa.rollback
                })
      .mapBoth(e => RepositoryError(e.getMessage), list => commands.size)//.as(params.size)
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

   def buildCmd[A, B](postgres: Resource[Task, Session[Task]], cmdx: Command[B]): Task[PreparedCommand[Task, B]] =
    postgres
      .use: session =>
        session
          .prepare(cmdx)


   def buildAllCmd[A](postgres: Resource[Task, Session[Task]],  commands: List[Command[A]]): Task[List[PreparedCommand[Task, A]]] =
    postgres
      .use: session =>
        commands.traverse: cmd =>
          session
            .prepare(cmd)

   def executeBatchWithTxT[A, B](postgres: Resource[Task, Session[Task]], params: List[A], cmdx: Command[B], encode: A => B): ZIO[Any, RepositoryError, Int] = for {
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
       .mapBoth(e => RepositoryError(e.getMessage), list => params.size).as(params.size)
   } yield u

