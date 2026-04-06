package com.kabasoft.iws.repository

import cats._
import cats.syntax.all._
import cats.effect.Resource
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{FinancialsTransaction, common}
import skunk._
import skunk.codec.all.{int8, text}
import skunk.implicits.sql
import zio.*
import zio.interop.catz._
import java.time.Instant

trait MasterfileCRUD:


  val sequenceQuery: Query[String, Long] = sql"SELECT nextval($text)".query(int8)

  def transact[A, B](s: Session[Task], listA: List[A], listB: List[B], insertA:Command[A], insertB:Command[B]): Task[Unit] =
    s.transaction.use: xa =>
      s.prepareR(insertA).use: pciCustomer =>
        s.prepareR(insertB).use: pciBankAcc =>
          tryExec(xa, pciCustomer, pciBankAcc, listA,listB)

  def transactM [A, B, C, D] (s: Session[Task], insertListA: List[A],  insertListB: List[B],  insertCmdA:Command[A], insertCmdB:Command[B]
                       , updateCmdA:Command[C],  updateCmdB:Command[D] ): Task[Unit] =
    s.transaction.use: xa =>
      s.prepareR(insertCmdA).use: pciCustomer =>
        s.prepareR(updateCmdA).use: pcuCustomer =>
          s.prepareR(insertCmdB).use: pciBankAcc =>
            s.prepareR(updateCmdB).use: pcuBankAcc =>
              tryExec(xa, pciCustomer, pciBankAcc, pcuCustomer, pcuBankAcc, insertListA, insertListB, List.empty, List.empty)

  def transact[A, B, C, D, E] (s: Session[Task], insertListA: List[A], insertListB: List[B], updateListA: List[C]
               , updateListB: List[D], deleteListB: List[E], insertCmdA:Command[A], insertCmdB:Command[B]
               , updateCmdA:Command[C],  updateCmdB:Command[D], deleteCmdB:Command[E]): Task[Unit] =
    s.transaction.use: xa =>
      s.prepareR(insertCmdA).use: insertPrepCmdA =>
        s.prepareR(insertCmdB).use: insertPrepCmdB =>
          s.prepareR(updateCmdA).use: updatePrepCmdA =>
            s.prepareR(updateCmdB).use: updatePrepCmdB =>
              s.prepareR(deleteCmdB).use: deletePrepCmdB =>
                tryExec(xa, insertPrepCmdA, insertPrepCmdB, updatePrepCmdA, updatePrepCmdB, deletePrepCmdB
                  , insertListA, insertListB, updateListA, updateListB, deleteListB)
                                 
  def buildId(transaction: FinancialsTransaction): FinancialsTransaction =
    if (transaction.id1 > 0L) transaction else {
      List(transaction).zipWithIndex.map { case (ftr, i) =>
        val idx = Instant.now().getNano + i.toLong
        ftr.copy(id1 = idx, lines = ftr.lines.map(_.copy(transid = idx)), period = common.getPeriod(ftr.transdate))
      }.headOption.getOrElse(transaction)
    }

  def exec[T](pc: PreparedCommand[Task, T], list: List[T]): Task[Unit] =
    list.traverse_ { p =>
      for
        _ <- pc.execute(p).debug("RRRRRRRR>>>")
      yield ()
    }
  def exec[T](session: Session[Task], pci: PreparedCommand[Task, T]
              , fn: (T, Long) => T, data: List[T], sequenceName:String):Task[Unit] =
    data.traverse { master =>
      for {
        transid <- session.unique(sequenceQuery)(sequenceName)
        _ <- pci.execute(fn(master, transid)).debug("MMMMM>>>")
      } yield ()
    }.map(_.headOption.getOrElse(()))

  def tryExec[T](xa: Transaction[Task], session: Session[Task], pci: PreparedCommand[Task, T], fm: (T, Long) => T
                 , models: List[T],  sequenceName: String): Task[Unit] =
    for
      sp <- xa.savepoint
      _ <- exec(session, pci, fm, models, sequenceName)
        .handleErrorWith(ex =>
          ZIO.logInfo(s"Unique violation: ${ex.getMessage}, rolling back...") *> xa.rollback(sp))
    yield ()
     
  def exec[T, D](masterPci: PreparedCommand[Task, T], detailsPci: PreparedCommand[Task, D], fm: (T, Long) => T
                 , fn: (T, Long) => List[D], fd: (D, Long) => D, data: List[T], session: Session[Task]
                 , sequenceName: (String, String)): ZIO[Any, Throwable, List[Unit]] = {
    data.traverse { master =>
      for {
        transid <- session.unique(sequenceQuery)(sequenceName._1)
        masterx = fm(master, transid)
        _ <- masterPci.execute(masterx) //.debug("MMMMM>>>")
        _ <- fn(masterx, transid).traverse_ { details =>
          for {
            id <- session.unique(sequenceQuery)(sequenceName._2)
            _ <- detailsPci.execute(fd(details, id)) //.debug("DDDDDDD>>>")
          } yield ()
        }
      } yield ()
    }
  }

  def execZ[A, B, C, D](session: Session[Task], masterPci: PreparedCommand[Task, A], detailsPci: PreparedCommand[Task, B]
                    , updatePcu: PreparedCommand[Task, D], fnSetMasterId: (A, Long) => A
                    , fnSetDetailsTransId: (A, Long) => List[B], fnSetDetailsId: (B, Long) => B //,fnMasterEncoder:A=>E
                    , fnUpdateEncoder:C=>D , data: List[A], updateData:List[C], sequenceName: (String, String)): ZIO[Any, Throwable, Unit] = for {
          _<- updateData.traverse { data2Update =>
            for {
              _<- updatePcu.execute(fnUpdateEncoder(data2Update))
            } yield ()
          }
          _<- data.traverse { master =>
                     for {
                          transid <- session.unique(sequenceQuery)(sequenceName._1)
                          masterx = fnSetMasterId(master, transid)
                                _ <- masterPci.execute(masterx).debug("MMMMM>>>")
                                _ <- fnSetDetailsTransId(masterx, transid).traverse_ {
                                  details =>
                                              for {
                                                    id <- session.unique(sequenceQuery)(sequenceName._2)
                                                     _ <- detailsPci.execute(fnSetDetailsId(details, id)).debug("DDDDDDD>>>")
                                              } yield ()
                                }
                    } yield ()
          }
       } yield ()

  def tryExec[A, B](xa: Transaction[Task], pciCustomer: PreparedCommand[Task, A]
                    , pciBankAcc: PreparedCommand[Task, B]
                    , customers: List[A], bankaccounts: List[B]): Task[Unit] =
    for
      sp <- xa.savepoint
      _ <- exec(pciCustomer, customers) *>
        exec(pciBankAcc, bankaccounts)
          .handleErrorWith(ex =>
            ZIO.logInfo(s"Unique violation: ${ex.getMessage}, rolling back...") *>
              xa.rollback(sp))
    yield ()



  def tryExec[A, B](xa: Transaction[Task], pciTransaction: PreparedCommand[Task, A]
                    , pciLine: PreparedCommand[Task, B], fm: (A, Long) => A
                    , fn:(A, Long)=>List[B], fd: (B, Long) => B, transactions: List[A] 
                    , session: Session[Task], sequenceName:(String, String)): Task[Unit] =
    for
      sp <- xa.savepoint
      _ <- exec(pciTransaction, pciLine,  fm, fn, fd, transactions, session, sequenceName) 
        .handleErrorWith(ex =>
          ZIO.logInfo(s"Unique violation: ${ex.getMessage}, rolling back...") *> xa.rollback(sp))
    yield ()
  
  // C= Customer.TYPE3
  // D= BankAccount.TYPE2
  def tryExec[A, B, C, D](xa: Transaction[Task], pciCustomer: PreparedCommand[Task, A]
              , pciBankAcc: PreparedCommand[Task, B]
              , pcuCustomer: PreparedCommand[Task, C]
              , pcuBankAcc: PreparedCommand[Task, D]
              , newCustomers: List[A], newBankaccounts: List[B] 
              , oldCustomers: List[C], oldBankaccounts: List[D]): Task[Unit] =
    for
      sp <- xa.savepoint
      _ <- exec(pciCustomer, newCustomers) *>
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
                             , newCustomers: List[A]
                             , newBankaccounts: List[B]
                             , oldCustomers: List[C]
                             , oldBankaccounts: List[D]
                             , bankacc2Delete: List[E]): Task[Unit] =

    for
      sp <- xa.savepoint
      _ <- exec(pciCustomer, newCustomers) *> //.debug("ZZZZZZZZ0>>>") *>
        exec(pciBankAcc, newBankaccounts) *> //.debug("ZZZZZZZZ1>>>") *>
        exec(pcuCustomer, oldCustomers) *> //.debug("ZZZZZZZZ2>>>") *>
        exec(pcuBankAcc, oldBankaccounts) *> //.debug("ZZZZZZZZ3>>>") *>
        exec(pcdBankAcc, bankacc2Delete) //.debug("ZZZZZZZZ4>>>")
          .handleErrorWith(ex =>
            ZIO.logInfo(s"Unique violation: ${ex.getMessage}, rolling back...") *>
              xa.rollback(sp))
    yield ()

  def tryExec1[A, B, C, D, E](xa: Transaction[Task]
                             , pciCustomer: PreparedCommand[Task, A]
                             , pciBankAcc: PreparedCommand[Task, B]
                             , pcuCustomer: PreparedCommand[Task, C]
                             , pcuBankAcc: PreparedCommand[Task, D]
                             , pcdBankAcc: PreparedCommand[Task, E]
                             , newCustomers: List[A]
                             , newBankaccounts: List[B]
                             , oldCustomers: List[C]
                             , oldBankaccounts: List[D]
                             , bankacc2Delete: List[E]): Task[Unit] =

    for
      sp <- xa.savepoint
      _ <- exec(pciCustomer, newCustomers) *> //.debug("ZZZZZZZZ0>>>") *>
        exec(pciBankAcc, newBankaccounts) *> //.debug("ZZZZZZZZ1>>>") *>
        exec(pcuCustomer, oldCustomers) *> //.debug("ZZZZZZZZ2>>>") *>
        exec(pcuBankAcc, oldBankaccounts) *> //.debug("ZZZZZZZZ3>>>") *>
        exec(pcdBankAcc, bankacc2Delete) //.debug("ZZZZZZZZ4>>>")
          .handleErrorWith(ex =>
            ZIO.logInfo(s"Unique violation: ${ex.getMessage}, rolling back...") *>
              xa.rollback(sp))
    yield ()

  def tryExec4[A, B, C, D, E, F, G](xa: Transaction[Task]
                                 , pciPac: PreparedCommand[Task, A]
                                 , pcuPac: PreparedCommand[Task, B]
                                 , pciFtr: PreparedCommand[Task, C]
                                 , pciLFtr: PreparedCommand[Task, D]
                                 , pcuFtr: PreparedCommand[Task, E]
                                 , pcuLFtr: PreparedCommand[Task, F]
                                 , pciJour: PreparedCommand[Task, G]
                                 , pac2Insert: List[A]
                                 , pac2update: List[B]
                                 , models2Insert: List[C]
                                 , lines2Insert: List[D]
                                 , models2Update: List[E] 
                                 , lines2update: List[F]
                                 , journals: List[G]): Task[Unit] =
    for
        sp <- xa.savepoint
        _ <- exec(pciPac, pac2Insert) *>
          exec(pciJour, journals) *>
          exec(pcuPac, pac2update) *>
          exec(pciFtr, models2Insert) *>
          exec(pciLFtr, lines2Insert) *>
          exec(pcuLFtr, lines2update) *>
          exec(pcuFtr, models2Update)
            .handleErrorWith(ex =>
              ZIO.logInfo(s"Unique violation: ${ex.getMessage}, rolling back...") *>
                xa.rollback(sp))
    yield ()

  def tryExec2[A, B, C, D, E, F, G, H, I, J](xa: Transaction[Task]
                                    , pciFtr: PreparedCommand[Task, A]
                                    , pciFtrDetails: PreparedCommand[Task, B]
                                    , pcuFtr: PreparedCommand[Task, C]
                                    , pciPac: PreparedCommand[Task, D]
                                    , pcuPac: PreparedCommand[Task, E]
                                    , pciStock: PreparedCommand[Task, F]
                                    , pcuStock: PreparedCommand[Task, G]
                                    , pciTransLog: PreparedCommand[Task, H]
                                    , pciJournal: PreparedCommand[Task, I]
                                    , pcuArt: PreparedCommand[Task, J]
                                    , newFtr: List[A]
                                    , newFTrDetails: List[B]
                                    , models: List[C]
                                    , newPacs:List[D], oldPacs:List[E]
                                    , newStock: List[F]
                                    , stock2update: List[G]
                                    , transLogEntries: List[H]
                                    , newJournals:List[I]
                                    , articles: List[J]
                                    ): Task[Unit] =
    for
      sp <- xa.savepoint
      _ <- exec(pciFtr, newFtr) *>
        exec(pciFtrDetails, newFTrDetails) *>
        exec(pcuFtr, models) *>
        exec(pciPac, newPacs) *>
        exec(pcuPac, oldPacs) *>
        exec(pciStock, newStock) *>
        exec(pcuStock, stock2update) *>
        exec(pciTransLog, transLogEntries) *>
        exec(pciJournal, newJournals) *>
        exec(pcuArt, articles)
          .handleErrorWith(ex =>
            ZIO.logInfo(s"Unique violation: ${ex.getMessage}, rolling back...") *>
              xa.rollback(sp))
    yield ()
    
  def tryExec3[A, B, C, D, E, F, G](xa: Transaction[Task]
                           , pciFtr: PreparedCommand[Task, A]
                           , pciFtrDetails: PreparedCommand[Task, B]
                          , pciStock: PreparedCommand[Task, C]
                          , pciTransLog: PreparedCommand[Task, D]
                          , pcuStock: PreparedCommand[Task, E]
                          , pcuArt: PreparedCommand[Task, F]
                          , pcuFtr: PreparedCommand[Task, G]
                          , newFtr: List[A]
                          , newFTrDetails: List[B]
                          , newStock: List[C]
                          , transLogEntries: List[D]
                          , stock2update: List[E]
                          , articles: List[F]
                          , models:List[G]): Task[Unit] =
    for
      sp <- xa.savepoint
      _ <-exec(pciFtr, newFtr) *>
        exec(pciFtrDetails, newFTrDetails) *>
        exec(pciStock, newStock) *>
        exec(pciTransLog, transLogEntries) *>
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

  def queryWithTxUniqueX[A, B](postgres: Resource[Task, Session[Task]], q: Query[A, B]): Task[PreparedQuery[Task, A, B]] =
    postgres
      .use: session =>
        session
          .prepare(q)

        
  def queryWithTxUnique[A, B](postgres: Resource[Task, Session[Task]], p:A, q:Query[A, B]):ZIO[Any, RepositoryError, B] =
     postgres
       .use: session =>
         session
          .prepare(q)
          .flatMap(ps => ps.unique(p)).debug("ZZZZZZZZZZZ")
       .mapBoth(e => RepositoryError(e.getMessage), a => a).debug("Data/Error")

  def queryWithTxUnique[ A](postgres: Resource[Task, Session[Task]],  q: Query[Void, A]): ZIO[Any, RepositoryError, A] =
    postgres.use: session =>
                session.unique(q)
            .mapBoth(e => RepositoryError(e.getMessage), a => a)//.debug("Data/Error")
  
  def executeWithTx[A](postgres: Resource[Task, Session[Task]], p: A, comd: Command[A], size: Int): ZIO[Any, RepositoryError, Int] =
    postgres
      .use: session =>
        session.transaction.use: xa =>
          session
            .prepare(comd).debug(s"ZZZZZZZZZ ${p}")
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
            .execute(cmd)//.debug("ffffffffffffffff")
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
      command.execute(p.map(encoder))//.debug("Executing")
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
              .prepare(comd)//.debug("SSSSSSSSSSSS")
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
  
  def execX[A, B, C, D, E, F](s: Session[Task], masterPci: PreparedCommand[Task, A], detailsPci: PreparedCommand[Task, B]
                 , masterPcu: PreparedCommand[Task, C], detailsPcu: PreparedCommand[Task, D], detailsPcd: PreparedCommand[Task, E]
                 , newMasterFilter: List[A]=> List[A], oldMasterFilter:List[A]=> List[A]
                 , newDetailsFilter: A => List[B], details2UpdateFilter: A => List[B], details2DeleteFilter: A => List[B]
                 , fnSetMasterId: (A, Long) => A, fnSetParentId: (A, Long) => List[B],  fnSetDetailsId: (B, Long) => B
                 , fnMasterUpdateEncoder:A=>C
                 , fnDetailsUpdateEncoder:B=>D, fnDetailsDeleteEncoder:B=>E
                 , data: List[A], sequenceName:(String, String)): ZIO[Any, Throwable, Unit] = {
                 newMasterFilter(data).traverse { master =>
                                  for {
                                        id <- s.unique(sequenceQuery)(sequenceName._1)
                                        masterx = fnSetMasterId(master, id)
                                              _ <- ZIO.logInfo(s"DDDDDDD>>> new master2Insert $masterx")
                                              _ <- masterPci.execute(masterx).debug("MMMMM>>>")
                                             _ <- fnSetParentId(masterx, id).traverse_  { details =>
                                                      for {
                                                            _ <- ZIO.logInfo(s"DDDDDDD>>> new details2Insert $details")
                                                            id <- s.unique(sequenceQuery)(sequenceName._2)
                                                            encodedDetails = fnSetDetailsId(details, id)
                                                              _ <- detailsPci.execute(encodedDetails).debug("DDDDDDD>>>")
                                                      } yield ()
                                                  }
                                  } yield ()
                 }
                 oldMasterFilter(data).traverse { master =>
                       for {
                                      _ <- ZIO.logInfo(s"DDDDDDD>>> master2Update $master")
                                      _ <- masterPcu.execute( fnMasterUpdateEncoder(master)).debug("MMMMM>>>")
                                      _ <- details2UpdateFilter(master).map(fnDetailsUpdateEncoder).traverse_ { details =>
                                              for {
                                                   _ <- ZIO.logInfo(s"DDDDDDD>>> details2Update $details")
                                                   _ <- detailsPcu.execute(details).debug("DDDDDDD>>>")
                                              } yield ()
                                        }
                                      _ <- ZIO.logInfo(s"DDDDDDD>>> newDetailsFilter ${newDetailsFilter(master)}")
                                      _ <- newDetailsFilter(master).traverse_ { details =>
                                              for {
                                                    id <- s.unique(sequenceQuery)(sequenceName._2)
                                                    _ <- ZIO.logInfo(s"DDDDDDD>>> newDetails ${fnSetDetailsId(details, id)}")
                                                    _ <- detailsPci.execute(fnSetDetailsId(details, id)).debug("DDDDDDD>>>")
                                              } yield ()
                                      }
                                      _ <- details2DeleteFilter(master).map(fnDetailsDeleteEncoder).traverse_ { details =>
                                              for {
                                                  _ <- ZIO.logInfo(s"DDDDDDD>>> details2Delete $details")
                                                  _ <- detailsPcd.execute(details).debug("DDDDDDD>>>")
                                              } yield ()
                                      }
                      } yield ()
                }
  }.map(_.headOption.getOrElse(()))

  def executeBatchWithTxK[A, B](postgres: Resource[Task, Session[Task]], params: List[A]
                                , cmdx: Command[B], encode: A => B): ZIO[Any, RepositoryError, Int] = for {
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