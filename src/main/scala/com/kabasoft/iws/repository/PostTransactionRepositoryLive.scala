package com.kabasoft.iws.repository
import cats.effect.Resource
import skunk.*
import zio.interop.catz.asyncInstance
import zio.{Task, ZIO, UIO, ZLayer}
import com.kabasoft.iws.domain.{Article, FinancialsTransaction, FinancialsTransactionDetails, Stock, Transaction
  , PeriodicAccountBalance, Journal, TransactionLog}
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.repository.FinancialsTransactionRepositorySQL.insertDetails


final case class PostTransactionRepositoryLive(postgres: Resource[Task, Session[Task]]) extends PostTransactionRepository, MasterfileCRUD:

  def transact(s: Session[Task], models: List[Transaction], financials: List[FinancialsTransaction], transLogEntries: List[TransactionLog]
               , stock2update: List[Stock], newStock: List[Stock], articles: List[Article]): Task[Unit] =
    s.transaction.use: xa =>
      s.prepareR(FinancialsTransactionRepositorySQL.insert).use: pciFtr =>
        s.prepareR(insertDetails).use: pciFtrDetails =>
          s.prepareR(TransactionLogRepositorySQL.insert).use: pciTransLog =>
            s.prepareR(StockRepositorySQL.insert).use: pciStock =>
              s.prepareR(StockRepositorySQL.UPDATE).use: pcuStock =>
                s.prepareR(ArticleRepositorySQL.UPDATE).use: pcuArt =>
                  s.prepareR(TransactionRepositorySQL.updatePosted).use: pcuFTr =>
                    tryExec3(xa, pciFtr, pciFtrDetails, pciStock, pciTransLog, pcuStock, pcuArt, pcuFTr
                      , financials, financials.flatMap(_.lines).map(FinancialsTransactionDetails.encodeIt4)
                      , newStock, transLogEntries, stock2update.map(Stock.encodeIt3), articles.map(Article.encodeIt2)
                      , models.map(Transaction.encodeIt3))
                  
  def transact(s: Session[Task], models: List[Transaction], financials: List[FinancialsTransaction]
               , newPacs:List[PeriodicAccountBalance], pac2update: List[PeriodicAccountBalance], transLogEntries: List[TransactionLog]
               , journals:List[Journal], stock2update: List[Stock], newStock: List[Stock], articles: List[Article] ): Task[Unit] =
    s.transaction.use: xa =>
      s.prepareR(FinancialsTransactionRepositorySQL.insert).use: pciFtr =>
        s.prepareR(insertDetails).use: pciFtrDetails =>
          s.prepareR(TransactionRepositorySQL.updatePosted).use: pcuFtr =>
             s.prepareR(PacRepositorySQL.insert).use: pciPac =>
               s.prepareR(PacRepositorySQL.UPDATE).use: pcuPac => 
                 s.prepareR(StockRepositorySQL.insert).use: pciStock =>
                   s.prepareR(StockRepositorySQL.UPDATE).use: pcuStock =>
                     s.prepareR(TransactionLogRepositorySQL.insert).use: pciTransLog => 
                       s.prepareR(JournalRepositorySQL.insert).use: pciJournal =>
                         s.prepareR(ArticleRepositorySQL.UPDATE).use: pcuArt =>
                          tryExec2(xa, pciFtr, pciFtrDetails, pcuFtr, pciPac, pcuPac
                          , pciStock,  pcuStock, pciTransLog, pciJournal, pcuArt, financials 
                           , financials.flatMap(_.lines).map(FinancialsTransactionDetails.encodeIt4)
                           ,  models.map(Transaction.encodeIt3), newPacs, pac2update.map(PeriodicAccountBalance.encodeIt2), newStock
                           ,  stock2update.map(Stock.encodeIt3), transLogEntries, journals
                           , articles.map(Article.encodeIt2))

  override def post(models: List[Transaction], financials: List[FinancialsTransaction]
                    //, newPac: List[PeriodicAccountBalance]
                    //, pac2update: UIO[List[PeriodicAccountBalance]]
                    , transLogEntries: List[TransactionLog]
                    //,  journals: List[Journal]
                    , stock2update: List[Stock]
                    , newStock: List[Stock], articles: List[Article]): ZIO[Any, RepositoryError, Int] =
    for {
      _ <- ZIO.logInfo(s" New Stock  to insert into DB ${newStock}")
      _ <- ZIO.logInfo(s" Old stock  to update in DB ${stock2update}")
      _ <- ZIO.logInfo(s" Transaction log  ${transLogEntries}")
      //_ <- ZIO.logInfo(s" Journal entries to insert into the DB  ${journals}")
      _ <- ZIO.logInfo(s" Transaction posted  ${models}")
      nr <- (postgres
        .use:
          session => transact(session, models, financials.map(buildId),   transLogEntries, stock2update, newStock, articles))
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
          session => transact(session, models, financials.map(buildId), newPacs, pac2updatex
            , transLogEntries, journals, stock2update, newStock, articles))
        .mapBoth(e => RepositoryError(e.getMessage), _ => models.flatMap(_.lines).size + models.size)
    } yield nr
object PostTransactionRepositoryLive:
  val live: ZLayer[Resource[Task, Session[Task]], RepositoryError, PostTransactionRepository] =
      ZLayer.fromFunction(new PostTransactionRepositoryLive(_))



