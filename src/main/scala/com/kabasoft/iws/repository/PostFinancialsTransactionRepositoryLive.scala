package com.kabasoft.iws.repository

import cats.effect.Resource
import skunk._
import zio.interop.catz._
import zio.{ZIO, _}
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{FinancialsTransaction, FinancialsTransactionDetails, Journal, PeriodicAccountBalance}
import FinancialsTransactionRepositoryLive.{ FINANCIAL_DETAIL_SEQUENCE_PREF, FINANCIAL_SEQUENCE_PREF, sequenceNames
  , newMasterFilter, master2master, master2Details, Details2Details, newDetailsFilter
  , details2DeleteFilter, details2UpdateFilter, setJournalId}
import JournalRepositoryLive.{JOURNAL_SEQUENCE_PREF, sequenceName}

final case class PostFinancialsTransactionRepositoryLive(postgres: Resource[Task, Session[Task]]) extends
                PostFinancialsTransactionRepository, MasterfileCRUD:
  
  def delete(p:(Long, Int, String)): ZIO[Any, RepositoryError, Int] =
    executeWithTx(postgres, p, FinancialsTransactionRepositorySQL.DELETE, 1)


  private def transactModifyInternal(s: Session[Task], models: List[FinancialsTransaction]
                                     , models2Update: List[FinancialsTransaction]
                                     , pac2Insert: List[PeriodicAccountBalance]
                                     , pac2update: List[PeriodicAccountBalance]
                                     , journals: List[Journal] ): Task[Int] = {
    val (seqMaster, seqDetail) = sequenceNames(FINANCIAL_SEQUENCE_PREF, FINANCIAL_DETAIL_SEQUENCE_PREF, models)
    val seqJournal = sequenceName(JOURNAL_SEQUENCE_PREF, journals)

    def nextId(company: String): Task[Long] = s.unique(sequenceQuery)(company)

    def exec[A](cmd: PreparedCommand[Task, A], value: A): Task[Unit] = cmd.execute(value).unit

    def withId[A](company: String, f: Long => A): Task[A] = nextId(company).map(f)

    (for {
      pciMaster <- s.prepareR(FinancialsTransactionRepositorySQL.insert)
      pcuMaster <- s.prepareR(FinancialsTransactionRepositorySQL.updatePosted)
      pciPac <- s.prepareR(PacRepositorySQL.insert)
      pcuPac <- s.prepareR(PacRepositorySQL.UPDATE)
      pciDetails <- s.prepareR(FinancialsTransactionRepositorySQL.insertDetails)
      pcuDetails <- s.prepareR(FinancialsTransactionRepositorySQL.UPDATE_DETAILS)
      pcdDetails <- s.prepareR(FinancialsTransactionRepositorySQL.DELETE_DETAILS)
      pciJour <- s.prepareR(JournalRepositorySQL.insert)

    } yield (pciMaster, pcuMaster, pciDetails, pcuDetails, pcdDetails, pciPac, pcuPac, pciJour)).use {
      case (pciMaster, pcuMaster, pciDetails, pcuDetails, pcdDetails, pciPac, pcuPac, pciJournal) =>
        for {
          newMasters <- ZIO.collectAll(
            newMasterFilter(models).map { master =>
              withId(seqMaster, id => master2master(master, id)).tap { m =>
                ZIO.logInfo(s"Insert new master: $m  ") *>
                  exec(pciMaster, m) *>
                  ZIO.foreachDiscard(master2Details(m, m.id)) { d =>
                    ZIO.logInfo(s"Insert new details: $d") *>
                      withId(seqDetail, id => Details2Details(d, id)).tap(exec(pciDetails, _))
                  }
              }
            }
          )

          updatedMasters <- ZIO.collectAll(
            models2Update.map { master =>
              ZIO.succeed(master).tap { m =>
                ZIO.logInfo(s"updating  old master: $m") *>
                  exec(pcuMaster, FinancialsTransaction.encodeIt3(m)) *>
                  ZIO.logInfo(s"updating  old details: ${details2UpdateFilter(m)}") *>
                  ZIO.foreachDiscard(details2UpdateFilter(m).map(FinancialsTransactionDetails.encodeIt2))(exec(pcuDetails, _)) *>
                  ZIO.foreachDiscard(newDetailsFilter(m)) { d =>
                    ZIO.logInfo(s"Inserting new details: $d") *>
                      withId(seqDetail, id => Details2Details(d, id)).tap(exec(pciDetails, _))
                  } *>
                  ZIO.logInfo(s"Deleting old details: ") *>
                  ZIO.foreachDiscard(details2DeleteFilter(m).map(FinancialsTransactionDetails.encodeIt3))(exec(pcdDetails, _))
              }
            }
          )
          journalEntries <- ZIO.collectAll(
            journals.map { journal =>
              withId(seqJournal, id => setJournalId(journal, id)).tap { m =>ZIO.logInfo(s"Insert new master: $m  ") *>
                exec(pciJournal, m)
              }
            }
          )
          _ <- ZIO.collectAll(pac2Insert.map { pac =>ZIO.logInfo(s"Insert new master: $pac  ") *> exec(pciPac, pac)})
          _ <- ZIO.collectAll(pac2update.map(PeriodicAccountBalance.encodeIt2)
            .map { pac =>ZIO.logInfo(s"Updating old pacs : $pac  ") *> exec(pcuPac, pac)})
        } yield newMasters.size + updatedMasters.size +pac2Insert.size+pac2update.size+journalEntries.size
    }
  }
  override def post(models2Insert: List[FinancialsTransaction], models2Update: List[FinancialsTransaction],
                    pac2Insert: List[PeriodicAccountBalance], pac2update: UIO[List[PeriodicAccountBalance]],
                    journals: List[Journal]): ZIO[Any, RepositoryError, Int] = 
      for {
          pac2updatex <- pac2update
                    _ <- ZIO.logInfo(s" New Pacs  to insert into DB ${pac2Insert}")
                    _ <- ZIO.logInfo(s" Old Pacs  to update in DB ${pac2updatex}")
                    _ <- ZIO.logInfo(s" journals  ${journals}")
                    _ <- ZIO.logInfo(s" inserted Transaction   ${models2Insert}")
                    _ <- ZIO.logInfo(s" updated  Transaction   ${models2Update}")
                    nr <-   (postgres
                              .use:
                                  session =>
                                    transactModifyInternal(session, models2Insert, models2Update
                                    , pac2Insert, pac2updatex, journals))
                              .mapBoth(e => RepositoryError(e.getMessage), _ => models2Insert.flatMap(_.lines).size
                                + models2Insert.size +pac2Insert.size+pac2updatex.size+journals.size)
     } yield nr

object PostFinancialsTransactionRepositoryLive:
    val live: ZLayer[Resource[Task, Session[Task]], Throwable, PostFinancialsTransactionRepository] =
      ZLayer.fromFunction(new PostFinancialsTransactionRepositoryLive(_))
    def sequenceName(prefix1: String, models: List[FinancialsTransaction]): String = {
        val model = models.headOption.getOrElse(FinancialsTransaction.dummy)
        val company: String = model.company
        s"${prefix1}_${company}"
    }
