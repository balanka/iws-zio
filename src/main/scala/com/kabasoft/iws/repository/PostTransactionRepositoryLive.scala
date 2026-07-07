package com.kabasoft.iws.repository
import cats.effect.Resource
import skunk._
import zio.interop.catz.asyncInstance
import zio.{Task, UIO, ZIO, ZLayer}
import com.kabasoft.iws.domain.{Article, FinancialsTransaction, FinancialsTransactionDetails, Journal
  , PeriodicAccountBalance, Stock, Transaction, TransactionDetails, TransactionLog}
import com.kabasoft.iws.domain.AppError.RepositoryError
import FinancialsTransactionRepositoryLive.{Details2Details, FINANCIAL_DETAIL_SEQUENCE_PREF, FINANCIAL_SEQUENCE_PREF
  , details2UpdateFilter, master2Details, master2master, newDetailsFilter, newMasterFilter, oldMasterFilter
  , sequenceNames, setJournalId}
import JournalRepositoryLive.{JOURNAL_SEQUENCE_PREF, sequenceName}
import TransactionRepositoryLive.{TRANSACTION_DETAIL_SEQUENCE_PREF, TRANSACTION_LOG_SEQUENCE_PREF
  , TRANSACTION_SEQUENCE_PREF, newTransactionDetailsFilter, newTransactionFilter, setDetailsId, setTransactionId
  , setTransactionLogId, transaction2Details, transactionDetails2UpdateFilter}



final case class PostTransactionRepositoryLive(postgres: Resource[Task, Session[Task]]) extends PostTransactionRepository, MasterfileCRUD:

  private def getFinancialsSequenz(prefixMaster:String, prefixDetails: String, models:List[FinancialsTransaction]) = sequenceNames (prefixMaster, prefixDetails, models)
  private def getTransactionSequenz(prefixMaster:String, prefixDetails: String, models:List[Transaction]) =
    TransactionRepositoryLive.sequenceNames (prefixMaster, prefixDetails, models)
  private def getJournalSequenz( journals:List[Journal])= sequenceName(JOURNAL_SEQUENCE_PREF, journals)
  private def getTransactionLogSequenz(transLogs:List[TransactionLog])=
       TransactionRepositoryLive.sequenceName(TRANSACTION_LOG_SEQUENCE_PREF, transLogs)


  def nextId(session:Session[Task],  company:String): Task[Long] = session.unique(sequenceQuery)(company)
  def exec[A](cmd: PreparedCommand[Task, A], value: A): Task[Unit] = cmd.execute(value).unit
  def withId[A](session:Session[Task], company: String, f: Long => A): Task[A] = nextId(session, company).map(f)

  private def transactModifyInternal(s: Session[Task], models: List[Transaction]
                                     , financials: List[FinancialsTransaction]
                                     , transLogs: List[TransactionLog]
                                     , stock2update: List[Stock], newStock: List[Stock]
                                     , articles: List[Article]): Task[Int] = {

    val (seqMaster, seqDetail) = getFinancialsSequenz(FINANCIAL_SEQUENCE_PREF, FINANCIAL_DETAIL_SEQUENCE_PREF, financials)
    val (seqTransaction, seqTransactionDetail) = getTransactionSequenz(TRANSACTION_SEQUENCE_PREF, TRANSACTION_DETAIL_SEQUENCE_PREF, models)
    val seqTransLog = getTransactionLogSequenz(transLogs)
    (for {
      pciTr <- s.prepareR(TransactionRepositorySQL.insert)
      pcuTr <- s.prepareR(TransactionRepositorySQL.updatePosted)
      pciFtr <- s.prepareR(FinancialsTransactionRepositorySQL.insert)
      pcuFtr <- s.prepareR(FinancialsTransactionRepositorySQL.updatePosted)
      pciStock <- s.prepareR(StockRepositorySQL.insert)
      pcuStock <- s.prepareR(StockRepositorySQL.UPDATE)
      pcuArt <- s.prepareR(ArticleRepositorySQL.UPDATE)
      pciFtrDetails <- s.prepareR(FinancialsTransactionRepositorySQL.insertDetails)
      pcuFtrDetails <- s.prepareR(FinancialsTransactionRepositorySQL.UPDATE_DETAILS)
      pciTrDetails <- s.prepareR(TransactionRepositorySQL.insertDetails)
      pcuTrDetails <- s.prepareR(TransactionRepositorySQL.UPDATE_DETAILS)
      pciTransLog <- s.prepareR(TransactionLogRepositorySQL.insert)

    } yield (pciTr, pcuTr, pciFtr, pcuFtr, pciFtrDetails, pcuFtrDetails, pciTrDetails, pcuTrDetails, pciStock, pcuStock, pcuArt, pciTransLog)).use {
      case (pciTr, pcuTr, pciFtr, pcuFtr, pciFtrDetails, pcuFtrDetails, pciTrDetails, pcuTrDetails, pciStock, pcuStock, pcuArt, pciTransLog) =>
        for {
          newFinancials <- ZIO.collectAll(
            newMasterFilter(financials).map { master =>
              withId(s, seqMaster, id => master2master(master, id)).tap { m =>
                ZIO.logInfo(s"Insert new master: $m  ") *>
                  exec(pciFtr, m) *>
                  ZIO.foreachDiscard(master2Details(m, m.id)) { d =>
                    ZIO.logInfo(s"Insert new details: $d") *>
                      withId(s, seqDetail, id => Details2Details(d, id)).tap(exec(pciFtrDetails, _))
                  }
              }
            }
          )
          updatedMasters <- ZIO.collectAll(
            oldMasterFilter(financials).map { master =>
              ZIO.succeed(master).tap { m =>
                ZIO.logInfo(s"updating  old financials: $m") *>
                  exec(pcuFtr, FinancialsTransaction.encodeIt3(m)) *>
                  ZIO.logInfo(s"updating  old financials details: ${details2UpdateFilter(m)}") *>
                  ZIO.foreachDiscard(details2UpdateFilter(m).map(FinancialsTransactionDetails.encodeIt2))(exec(pcuFtrDetails, _)) *>
                  ZIO.foreachDiscard(newDetailsFilter(m)) { d =>
                    ZIO.logInfo(s"Inserting new financials details: $d") *>
                      withId(s, seqDetail, id => Details2Details(d, id)).tap(exec(pciFtrDetails, _))
                  }
              }
            }
          )

          newTransactions <- ZIO.collectAll(
            newTransactionFilter(models).map { transaction =>
              withId(s, seqTransaction, id => setTransactionId(transaction, id)).tap { m =>
                ZIO.logInfo(s"Insert new transaction: $m  ") *>
                  exec(pciTr, m) *>
                  ZIO.foreachDiscard(transaction2Details(m, m.id)) { d =>
                    ZIO.logInfo(s"Insert new details: $d") *>
                      withId(s, seqDetail, id => setDetailsId(d, id)).tap(exec(pciTrDetails, _))
                  }
              }
            }
          )

          updatedTransaction <- ZIO.collectAll(
            models.map { master =>
              ZIO.succeed(master).tap { m =>
                ZIO.logInfo(s"updating  old transaction: $m") *>
                  exec(pcuTr, Transaction.encodeIt3(m)) *>
                  ZIO.logInfo(s"updating  old transaction details: ${transactionDetails2UpdateFilter(m)}") *>
                  ZIO.foreachDiscard(transactionDetails2UpdateFilter(m).map(TransactionDetails.encodeIt2))(exec(pcuTrDetails, _)) *>
                  ZIO.foreachDiscard(newTransactionDetailsFilter(m)) { d =>
                    ZIO.logInfo(s"Inserting new transaction details: $d") *>
                      withId(s, seqTransactionDetail, id => setDetailsId(d, id)).tap(exec(pciTrDetails, _))
                  } //*>
                //ZIO.logInfo(s"Deleting old financials details: ") *>
                //ZIO.foreachDiscard(details2DeleteFilter(m).map(FinancialsTransactionDetails.encodeIt3))(exec(pcdDetails, _))
              }
            }
          )

          transLogEntries <- ZIO.collectAll(
            transLogs.map { transLog =>
              withId(s, seqTransLog, id => setTransactionLogId(transLog, id)).tap { m =>
                ZIO.logInfo(s"Insert new ransactionLog: $m  ") *> exec(pciTransLog, m)
              }
            }
          )
          _ <- ZIO.collectAll(newStock.map { stock => ZIO.logInfo(s"Insert new stock: $stock  ") *> exec(pciStock, stock) })
          _ <- ZIO.collectAll(stock2update.map(Stock.encodeIt3)
            .map { stock => ZIO.logInfo(s"Updating old stock : $stock  ") *> exec(pcuStock, stock) })
          _ <- ZIO.collectAll(articles.map(Article.encodeIt2)
            .map { article => ZIO.logInfo(s"Updating  article : $article  ") *> exec(pcuArt, article) })
        } yield newFinancials.size + updatedMasters.size  + newTransactions.size + updatedTransaction.size+articles.size + transLogEntries.size
    }
  }


  private def transactModifyInternal(s: Session[Task], models: List[Transaction]
                                     , financials: List[FinancialsTransaction]
                                     , newPacs:List[PeriodicAccountBalance], pac2update: List[PeriodicAccountBalance], transLogs: List[TransactionLog]
                                     , journals:List[Journal], stock2update: List[Stock], newStock: List[Stock], articles: List[Article] ): Task[Int] = {
    val (seqMaster, seqDetail) = getFinancialsSequenz(FINANCIAL_SEQUENCE_PREF, FINANCIAL_DETAIL_SEQUENCE_PREF, financials)
    val (seqTransaction, seqTransactionDetail) = getTransactionSequenz(TRANSACTION_SEQUENCE_PREF, TRANSACTION_DETAIL_SEQUENCE_PREF, models)
    val seqTransLog = getTransactionLogSequenz(transLogs)
    val seqJournal = getJournalSequenz( journals)
    (for {
      pciTr <- s.prepareR(TransactionRepositorySQL.insert)
      pcuTr <- s.prepareR(TransactionRepositorySQL.updatePosted)
      pciFtr <- s.prepareR(FinancialsTransactionRepositorySQL.insert)
      pcuFtr <- s.prepareR(FinancialsTransactionRepositorySQL.updatePosted)
      pciPac <- s.prepareR(PacRepositorySQL.insert)
      pcuPac <- s.prepareR(PacRepositorySQL.UPDATE)
      pciStock <- s.prepareR(StockRepositorySQL.insert)
      pcuStock <- s.prepareR(StockRepositorySQL.UPDATE)
      pcuArt <- s.prepareR(ArticleRepositorySQL.UPDATE)
      pciFtrDetails <- s.prepareR(FinancialsTransactionRepositorySQL.insertDetails)
      pcuFtrDetails <- s.prepareR(FinancialsTransactionRepositorySQL.UPDATE_DETAILS)
      pciTrDetails <- s.prepareR(TransactionRepositorySQL.insertDetails)
      pcuTrDetails <- s.prepareR(TransactionRepositorySQL.UPDATE_DETAILS)
      pciJournal <- s.prepareR(JournalRepositorySQL.insert)
      pciTransLog <- s.prepareR(TransactionLogRepositorySQL.insert)

    } yield (pciTr, pcuTr, pciFtr, pcuFtr, pciFtrDetails, pcuFtrDetails, pciTrDetails, pcuTrDetails, pciPac, pcuPac
      , pciStock, pcuStock, pcuArt, pciJournal, pciTransLog)).use {
      case (pciTr, pcuTr, pciFtr, pcuFtr, pciFtrDetails, pcuFtrDetails, pciTrDetails, pcuTrDetails,pciPac, pcuPac
      , pciStock, pcuStock, pcuArt, pciJournal, pciTransLog) =>
        for {
          newFinancials <- ZIO.collectAll(
            newMasterFilter(financials).map { master =>
              withId(s, seqMaster, id => master2master(master, id)).tap { m =>
                ZIO.logInfo(s"Insert new master: $m  ") *>
                  exec(pciFtr, m) *>
                  ZIO.foreachDiscard(master2Details(m, m.id)) { d =>
                    ZIO.logInfo(s"Insert new details: $d") *>
                      withId(s, seqDetail, id => Details2Details(d, id)).tap(exec(pciFtrDetails, _))
                  }
              }
            }
          )

          updatedMasters <- ZIO.collectAll(
            oldMasterFilter(financials).map { master =>
              ZIO.succeed(master).tap { m =>
                ZIO.logInfo(s"updating  old financials: $m") *>
                  exec(pcuFtr, FinancialsTransaction.encodeIt3(m)) *>
                  ZIO.logInfo(s"updating  old financials details: ${details2UpdateFilter(m)}") *>
                  ZIO.foreachDiscard(details2UpdateFilter(m).map(FinancialsTransactionDetails.encodeIt2))(exec(pcuFtrDetails, _)) *>
                  ZIO.foreachDiscard(newDetailsFilter(m)) { d =>
                    ZIO.logInfo(s"Inserting new financials details: $d") *>
                      withId(s, seqDetail, id => Details2Details(d, id)).tap(exec(pciFtrDetails, _))
                  } //*>
                    //ZIO.logInfo(s"Deleting old financials details: ") *>
                   //ZIO.foreachDiscard(details2DeleteFilter(m).map(FinancialsTransactionDetails.encodeIt3))(exec(pcdDetails, _))
              }
            }
          )
          newTransactions <- ZIO.collectAll(
            newTransactionFilter(models).map { transaction =>
              withId(s, seqTransaction, id => setTransactionId(transaction, id)).tap { m =>
                ZIO.logInfo(s"Insert new transaction: $m  ") *>
                  exec(pciTr, m) *>
                  ZIO.foreachDiscard(transaction2Details(m, m.id)) { d =>
                    ZIO.logInfo(s"Insert new details: $d") *>
                      withId(s, seqDetail, id => setDetailsId(d, id)).tap(exec(pciTrDetails, _))
                  }
              }
            }
          )

          updatedTransaction <- ZIO.collectAll(
            models.map { master =>
              ZIO.succeed(master).tap { m =>
                ZIO.logInfo(s"updating  old transaction: $m") *>
                  exec(pcuTr, Transaction.encodeIt3(m)) *>
                  ZIO.logInfo(s"updating  old transaction details: ${transactionDetails2UpdateFilter(m)}") *>
                  ZIO.foreachDiscard(transactionDetails2UpdateFilter(m).map(TransactionDetails.encodeIt2))(exec(pcuTrDetails, _)) *>
                  ZIO.foreachDiscard(newTransactionDetailsFilter(m)) { d =>
                    ZIO.logInfo(s"Inserting new transaction details: $d") *>
                      withId(s, seqTransactionDetail, id => setDetailsId(d, id)).tap(exec(pciTrDetails, _))
                  } //*>
                //ZIO.logInfo(s"Deleting old financials details: ") *>
                //ZIO.foreachDiscard(details2DeleteFilter(m).map(FinancialsTransactionDetails.encodeIt3))(exec(pcdDetails, _))
              }
            }
          )

          journalEntries <- ZIO.collectAll(
            journals.map { journal =>
              withId(s, seqJournal, id => setJournalId(journal, id)).tap { m =>
                ZIO.logInfo(s"Insert new master: $m  ") *>
                  exec(pciJournal, m)
              }
            }
          )
          transLogEntries <- ZIO.collectAll(
            transLogs.map { transLog =>
              withId(s, seqTransLog, id => setTransactionLogId(transLog, id)).tap { m =>
                ZIO.logInfo(s"Insert new ransactionLog: $m  ") *> exec(pciTransLog, m)}
            }
          )
          _ <- ZIO.collectAll(newPacs.map { pac => ZIO.logInfo(s"Insert new master: $pac  ") *> exec(pciPac, pac) })
          _ <- ZIO.collectAll(pac2update.map(PeriodicAccountBalance.encodeIt2)
                  .map { pac => ZIO.logInfo(s"Updating old pacs : $pac  ") *> exec(pcuPac, pac) })
          _ <- ZIO.collectAll(newStock.map { stock => ZIO.logInfo(s"Insert new stock: $stock  ") *> exec(pciStock, stock) })
          _ <- ZIO.collectAll(stock2update.map(Stock.encodeIt3)
               .map { stock => ZIO.logInfo(s"Updating old stock : $stock  ") *> exec(pcuStock, stock) })
          _ <- ZIO.collectAll(articles.map(Article.encodeIt2)
                  .map { article => ZIO.logInfo(s"Updating  article : $article  ") *> exec(pcuArt, article) })
        } yield newFinancials.size + updatedMasters.size + newPacs.size + pac2update.size + articles.size
          + journalEntries.size+transLogEntries.size+newTransactions.size+updatedTransaction.size
    }
  }
  override def post(models: List[Transaction], financials: List[FinancialsTransaction]
                    , transLogEntries: List[TransactionLog], stock2update: List[Stock]
                    , newStock: List[Stock], articles: List[Article]): ZIO[Any, RepositoryError, Int] =
    for {
      _ <- ZIO.logInfo(s" New Stock  to insert into DB ${newStock}")
      _ <- ZIO.logInfo(s" Old stock  to update in DB ${stock2update}")
      _ <- ZIO.logInfo(s" Transaction log  ${transLogEntries}")
      _ <- ZIO.logInfo(s" Transaction posted  ${models}")
      nr <- (postgres
        .use:
          session => transactModifyInternal(session, models, financials, transLogEntries, stock2update, newStock, articles))
           .mapBoth(e => RepositoryError(e.getMessage), _ => models.flatMap(_.lines).size + models.size)
    } yield nr
                        
  override def post(models: List[Transaction], financials: List[FinancialsTransaction]
                    , newPacs: List[PeriodicAccountBalance]
                    , pac2update: UIO[List[PeriodicAccountBalance]]
                    , transLogEntries: List[TransactionLog]
                    , journals: List[Journal]
                    , stock2update: List[Stock]
                    , newStock: List[Stock], articles: List[Article]): ZIO[Any, RepositoryError, Int] =
    for {
      pac2updatex <- pac2update
      _ <- ZIO.logInfo(s" New PACS  to insert into DB ${newPacs}")
      _ <- ZIO.logInfo(s" Old PACS  to update in DB ${pac2update}")
      _ <- ZIO.logInfo(s" New Stock  to insert into DB ${newStock}")
      _ <- ZIO.logInfo(s" Old stock  to update in DB ${stock2update}")
      _ <- ZIO.logInfo(s" Transaction log  ${transLogEntries}")
      _ <- ZIO.logInfo(s" Journal entries to insert into the DB  ${journals}")
      _ <- ZIO.logInfo(s" Transaction posted  ${models}")
      nr <- (postgres
        .use:
          session => transactModifyInternal(session, models, financials, newPacs, pac2updatex
            , transLogEntries, journals, stock2update, newStock, articles))
        .mapBoth(e => RepositoryError(e.getMessage), _ => models.flatMap(_.lines).size + models.size)
    } yield nr
object PostTransactionRepositoryLive:
  val live: ZLayer[Resource[Task, Session[Task]], RepositoryError, PostTransactionRepository] =
      ZLayer.fromFunction(new PostTransactionRepositoryLive(_))



