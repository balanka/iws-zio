package com.kabasoft.iws.repository

import cats._
import cats.syntax.all._
import cats.effect.Resource
import com.kabasoft.iws.domain.AppError.RepositoryError
import skunk._
import skunk.codec.all.{int8, text}
import skunk.implicits.sql
import zio.*
import zio.interop.catz._

trait MasterfileCRUD:

  // ── Sequence generator ────────────────────────────────────────────────

  val sequenceQuery: Query[String, Long] = sql"SELECT nextval($text)".query(int8)

  /** Insert masters + details in a caller-supplied session,
   * in ONE transaction. */

  def transact[A, B](
                      s: Session[Task],
                      listA: List[A],
                      listB: List[B],
                      insertA: Command[A],
                      insertB: Command[B]
                    ): Task[Unit] =
    s.transaction.use { _ =>
      for {
        pciA <- s.prepare(insertA)
        pciB <- s.prepare(insertB)
        _    <- exec(pciA, listA)
        _    <- exec(pciB, listB)
      } yield ()
    }

  def transact[A, B, C, D, E](
                               s: Session[Task],
                               insertListA: List[A],
                               insertListB: List[B],
                               updateListA: List[C],
                               updateListB: List[D],
                               deleteListB: List[E],
                               insertCmdA: Command[A],
                               insertCmdB: Command[B],
                               updateCmdA: Command[C],
                               updateCmdB: Command[D],
                               deleteCmdB: Command[E]
                             ): Task[Unit] =
    s.transaction.use { _ =>
      s.prepareR(insertCmdA).use { pciA =>
        s.prepareR(insertCmdB).use { pciB =>
          s.prepareR(updateCmdA).use { pcuA =>
            s.prepareR(updateCmdB).use { pcuB =>
              s.prepareR(deleteCmdB).use { pcdB =>
                exec(pciA, insertListA) *>
                  exec(pciB, insertListB) *>
                  exec(pcuA, updateListA) *>
                  exec(pcuB, updateListB) *>
                  exec(pcdB, deleteListB)
              }
            }
          }
        }
      }
    }


  // ── Execute a prepared command N times ────────────────────────────────

  /** Execute a prepared command once per element. Propagates the first
   * failure. Must be called inside an open `session.transaction.use`. */
  def exec[T](pc: PreparedCommand[Task, T], list: List[T]): Task[Unit] =
    list.traverse_(p => pc.execute(p))

  /** Execute a prepared command once per element, generating a sequence
   * value per element and letting the caller thread it into the row. */
  def exec[T](
               session: Session[Task],
               pci:     PreparedCommand[Task, T],
               fn:      (T, Long) => T,
               data:    List[T],
               sequenceName: String
             ): Task[Unit] =
    data.traverse_ { master =>
      for {
        transid <- session.unique(sequenceQuery)(sequenceName)
        _       <- pci.execute(fn(master, transid))
      } yield ()
    }

  // ── Master / details insert ───────────────────────────────────────────

  /** Insert masters and their details inside the caller's transaction.
   * Uses a per-master savepoint so that a single master's failure does
   * NOT abort the whole batch — it just skips that master. Every other
   * error propagates to the caller. */
  def exec[T, D](
                  xa:        Transaction[Task],
                  session:   Session[Task],
                  masterPci: PreparedCommand[Task, T],
                  detailsPci: PreparedCommand[Task, D],
                  fm:        (T, Long) => T,
                  fn:        (T, Long) => List[D],
                  fd:        (D, Long) => D,
                  data:      List[T],
                  sequenceName: (String, String)
                ): ZIO[Any, RepositoryError, List[T]] =
    data
      .traverse { master =>
        for {
          sp      <- xa.savepoint
          transid <- session.unique(sequenceQuery)(sequenceName._1)
          masterx  = fm(master, transid)
          result <- (for {
            _ <- masterPci.execute(masterx)
            _ <- fn(masterx, transid).traverse_ { details =>
              for {
                id <- session.unique(sequenceQuery)(sequenceName._2)
                _  <- detailsPci.execute(fd(details, id))
              } yield ()
            }
          } yield masterx)
            .tapError(e =>
              ZIO.logInfo(s"insert failed for ${masterx}: ${e.getMessage}, rolling back savepoint") *>
                xa.rollback(sp))
        } yield result
      }
      .mapError(e => RepositoryError(e.getMessage))

  // ── Batch insert / update / delete of masters and details ─────────────

  /** The general "master + details" transaction:
   *   - inserts new masters and their details
   *   - updates existing masters and their details
   *   - deletes details
   * All inside ONE transaction owned by the caller (use `postgres.use(s =>
          * s.transaction.use(xa => …))`). Any failure rolls back everything. */
  def execX[A, B, C, D, E](
                            xa:             Transaction[Task],
                            s:              Session[Task],
                            masterPci:      PreparedCommand[Task, A],
                            detailsPci:     PreparedCommand[Task, B],
                            masterPcu:      PreparedCommand[Task, C],
                            detailsPcu:     PreparedCommand[Task, D],
                            detailsPcd:     PreparedCommand[Task, E],
                            newMasterFilter:   List[A] => List[A],
                            oldMasterFilter:   List[A] => List[A],
                            newDetailsFilter:  A => List[B],
                            details2UpdateFilter: A => List[B],
                            details2DeleteFilter: A => List[B],
                            fnSetMasterId:  (A, Long) => A,
                            fnSetParentId:  (A, Long) => List[B],
                            fnSetDetailsId: (B, Long) => B,
                            fnMasterUpdateEncoder: A => C,
                            fnDetailsUpdateEncoder: B => D,
                            fnDetailsDeleteEncoder: B => E,
                            data:          List[A],
                            sequenceName:  (String, String)
                          ): ZIO[Any, RepositoryError, Unit] =
    (for {
      // 1. Insert new masters and their new details
      _ <- newMasterFilter(data).traverse_ { master =>
        for {
          id      <- s.unique(sequenceQuery)(sequenceName._1)
          masterx  = fnSetMasterId(master, id)
          _       <- masterPci.execute(masterx)
          _       <- fnSetParentId(masterx, id).traverse_ { details =>
            for {
              id      <- s.unique(sequenceQuery)(sequenceName._2)
              encoded  = fnSetDetailsId(details, id)
              _       <- detailsPci.execute(encoded)
            } yield ()
          }
        } yield ()
      }

      // 2. Update existing masters
      _ <- oldMasterFilter(data).traverse_ { master =>
        for {
          _ <- masterPcu.execute(fnMasterUpdateEncoder(master))
          _ <- details2UpdateFilter(master).map(fnDetailsUpdateEncoder).traverse_ { d =>
            detailsPcu.execute(d)
          }
          // 3. Insert new details for an existing master
          _ <- newDetailsFilter(master).traverse_ { details =>
            for {
              id      <- s.unique(sequenceQuery)(sequenceName._2)
              encoded  = fnSetDetailsId(details, id)
              _       <- detailsPci.execute(encoded)
            } yield ()
          }
          // 4. Delete details marked for removal
          _ <- details2DeleteFilter(master).map(fnDetailsDeleteEncoder).traverse_ { d =>
            detailsPcd.execute(d)
          }
        } yield ()
      }
    } yield ()).mapError(e => RepositoryError(e.getMessage))

  // ── Bulk exec used by the batch-command transaction ────────────────────

  /** Run every (command, arg) pair inside ONE transaction.
   * Used by `CompanyRepository.create` to build a company from scratch. */
  def executeWithTx(
                     postgres: Resource[Task, Session[Task]],
                     commands: List[(Command[Any], Any)]
                   ): Task[Int] =
    postgres.use { session =>
      session.transaction.use { _ =>
        ZIO
          .foreachDiscard(commands) { case (command, arg) =>
            session.prepare(command).flatMap(_.execute(arg))
          }
          .as(commands.size)
      }
    }

  // ── Single-command transaction helpers ────────────────────────────────

  /** Insert/update/delete a single row in a fresh transaction. */
  def executeWithTx[A](
                        postgres: Resource[Task, Session[Task]],
                        p:        A,
                        comd:     Command[A],
                        size:     Int
                      ): ZIO[Any, RepositoryError, Int] =
    postgres
      .use { session =>
        session.transaction.use { _ =>
          session.prepare(comd).flatMap(_.execute(p)).as(size)
        }
      }
      .mapError(e => RepositoryError(e.getMessage))

  /** Execute a `Command[Void]` (e.g. `CREATE SEQUENCE`) in a fresh
   * transaction. */
  def executeWithTx(
                     postgres: Resource[Task, Session[Task]],
                     cmd:      Command[Void],
                     size:     Int
                   ): ZIO[Any, RepositoryError, Int] =
    postgres
      .use { session =>
        session.transaction.use { _ =>
          session.execute(cmd).as(size)
        }
      }
      .mapError(e => RepositoryError(e.getMessage))

  /** Execute a command in a caller-owned session (single transaction). */
  def executeWithTx[A](
                        session: Session[Task],
                        p:       A,
                        comd:    Command[A],
                        size:    Int
                      ): Task[Int] =
    session.transaction.use { _ =>
      session.prepare(comd).flatMap(_.execute(p)).as(size)
    }

  /** Execute a prepared command inside a caller-owned transaction. */
  def executeWithTx[A](xa:      Transaction[Task], command: PreparedCommand[Task, A], p:       A): Task[Int] =
    command.execute(p).as(1)

  /** Execute a prepared command with a list of params inside a caller-
   * owned transaction. */
  def executeWithTx[A, B](
                           xa:      Transaction[Task],
                           command: PreparedCommand[Task, List[B]],
                           p:       List[A],
                           encoder: A => B
                         ): Task[Int] =
    command.execute(p.map(encoder)).as(p.size)

  /** Execute a command with a custom encoder in a fresh transaction. */
  def executeWithTx[A, B](
                           postgres: Resource[Task, Session[Task]],
                           p:        A,
                           encoder:  A => B,
                           comd:     Command[B],
                           size:     Int
                         ): ZIO[Any, RepositoryError, Int] =
    postgres
      .use { session =>
        session.transaction.use { _ =>
          session.prepare(comd).flatMap(_.execute(encoder(p))).as(size)
        }
      }
      .mapError(e => RepositoryError(e.getMessage))

  /** Execute a batch of a single command type in ONE transaction. */
  def executeBatchWithTxK[A, B](
                                 postgres: Resource[Task, Session[Task]],
                                 params:   List[A],
                                 cmdx:     Command[B],
                                 encode:   A => B
                               ): ZIO[Any, RepositoryError, Int] =
    postgres
      .use { session =>
        session.transaction.use { _ =>
          session
            .prepare(cmdx)
            .flatMap { cmd =>
              params.traverse_(p => cmd.execute(encode(p)))
            }
            .as(params.size)
        }
      }
      .mapError(e => RepositoryError(e.getMessage))

  // ── Query helpers ─────────────────────────────────────────────────────

  /** Execute updates then master+details inserts inside the **caller's**
   * transaction. This method **does not** open a transaction and **does
   * not** use savepoints — it is strictly all-or-nothing inside whatever
   * transaction the caller has opened. Any failure aborts the caller's
   * transaction, which rolls back every statement executed here.
   *
   * `_xa` is not referenced in the body — it exists purely as a
   * compile-time guard so `execZ` cannot be invoked outside an open
   * `Transaction[Task]`. All statements execute on `session`, the same
   * session `_xa` was opened on. */
  def execZ[A, B, C, D](
                         _xa: Transaction[Task],
                         session: Session[Task],
                         masterPci: PreparedCommand[Task, A],
                         detailsPci: PreparedCommand[Task, B],
                         updatePcu: PreparedCommand[Task, D],
                         fnSetMasterId: (A, Long) => A,
                         fnSetDetailsTransId: (A, Long) => List[B],
                         fnSetDetailsId: (B, Long) => B,
                         fnUpdateEncoder: C => D,
                         data: List[A],
                         updateData: List[C],
                         sequenceName: (String, String)
                       ): Task[Unit] =
    for {
      // 1. Update existing rows first.
      _ <- updateData.traverse_(d => updatePcu.execute(fnUpdateEncoder(d)))

      // 2. Insert new masters + their details.
      _ <- data.traverse_ { master =>
        for {
          masterId <- session.unique(sequenceQuery)(sequenceName._1)
          masterWithId = fnSetMasterId(master, masterId)
          _ <- masterPci.execute(masterWithId)
          _ <- fnSetDetailsTransId(masterWithId, masterId).traverse_ { details =>
            for {
              detailId <- session.unique(sequenceQuery)(sequenceName._2)
              _ <- detailsPci.execute(fnSetDetailsId(details, detailId))
            } yield ()
          }
        } yield ()
      }
    } yield ()

  def queryWithTx[A, B](
                         postgres: Resource[Task, Session[Task]],
                         p:        A,
                         q:        Query[A, B]
                       ): ZIO[Any, RepositoryError, List[B]] =
    postgres
      .use { session =>
        session.prepare(q).flatMap(_.stream(p, 1024).compile.toList)
      }
      .mapError(e => RepositoryError(e.getMessage))

  def queryWithTxUniqueX[A, B](
                                postgres: Resource[Task, Session[Task]],
                                q:        Query[A, B]
                              ): Task[PreparedQuery[Task, A, B]] =
    postgres.use(session => session.prepare(q))

  def queryWithTxUnique[A, B](
                               postgres: Resource[Task, Session[Task]],
                               p:        A,
                               q:        Query[A, B]
                             ): ZIO[Any, RepositoryError, B] =
    postgres
      .use(_.prepare(q).flatMap(_.unique(p)))
      .mapError(e => RepositoryError(e.getMessage))

  def queryWithTxUnique[A](
                            postgres: Resource[Task, Session[Task]],
                            q:        Query[Void, A]
                          ): ZIO[Any, RepositoryError, A] =
    postgres
      .use(_.unique(q))
      .mapError(e => RepositoryError(e.getMessage))