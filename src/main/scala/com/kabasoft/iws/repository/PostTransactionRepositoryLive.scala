package com.kabasoft.iws.repository
import cats.effect.Resource
import cats.syntax.all.*
import cats._
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*
import zio.prelude.FlipOps
import zio.interop.catz.*
import zio.stream.interop.fs2z.*
import zio.{Task, ZIO, UIO, ZLayer}
import com.kabasoft.iws.domain.{Article,  Masterfile, PeriodicAccountBalance, Journal, Stock, Transaction, TransactionLog }
import com.kabasoft.iws.domain.AppError.RepositoryError
import MasterfileCRUD.{UpdateCommand, InsertBatch}
import java.time.{Instant, LocalDateTime, ZoneId}

final case class PostTransactionRepositoryLive(postgres: Resource[Task, Session[Task]]) extends PostTransactionRepository, MasterfileCRUD:
  
  def transact(s: Session[Task], models: List[Transaction], newPac: List[PeriodicAccountBalance]
               , pac2update: List[PeriodicAccountBalance], journals: List[Journal]
               , transLogEntries: List[TransactionLog], stock2update: List[Stock], newStock: List[Stock]
               , articles: List[Article] ): Task[Unit] =
    s.transaction.use: xa =>
      s.prepareR(PacRepositorySQL.insert).use: pciPac =>
        s.prepareR(PacRepositorySQL.UPDATE).use: pcuPac =>
          s.prepareR(JournalRepositorySQL.insert).use: pciJour =>
            s.prepareR(TransactionLogRepositorySQL.insert).use: pciTransLog =>
              s.prepareR(StockRepositorySQL.insert).use: pciStock =>
                s.prepareR(StockRepositorySQL.UPDATE).use: pcuStock =>
                  s.prepareR(ArticleRepositorySQL.UPDATE).use: pcuArt =>
                    s.prepareR(TransactionRepositorySQL.updatePosted).use: pcuFTr =>
                      tryExec3(xa, pciPac, pciStock, pciJour, pciTransLog, pcuPac, pcuStock, pcuArt, pcuFTr, newPac
                        , newStock, journals, transLogEntries, pac2update.map(PeriodicAccountBalance.encodeIt2)
                        , stock2update.map(Stock.encodeIt3), articles.map(Article.encodeIt2), models.map(Transaction.encodeIt3))
  override def post(models: List[Transaction]
                    , newPac: List[PeriodicAccountBalance]
                    , pac2update: UIO[List[PeriodicAccountBalance]]
                    , transLogEntries: List[TransactionLog],
                      journals: List[Journal]
                    , stock2update: List[Stock]
                    , newStock: List[Stock]
                    , articles: List[Article]): ZIO[Any, RepositoryError, Int] =
       for {
          pac2updatex <- pac2update
//                      _ <- ZIO.when(newPac.nonEmpty)(ZIO.logInfo(s" New Pacs  to insert into DB ${newPac}"))
//                      _ <- ZIO.when(pac2updatex.nonEmpty)(ZIO.logInfo(s" Old Pacs  to update in DB ${pac2updatex}"))
//                      _ <- ZIO.when(transLogEntries.nonEmpty)(ZIO.logInfo(s" Transaction log  ${transLogEntries}"))
//                      _ <- ZIO.when(journals.nonEmpty)(ZIO.logInfo(s" journals  ${journals}"))
//                      _ <- ZIO.logInfo(s" Transaction posted  ${models}")
                      nr <-   (postgres
                    .use:
                      session =>
                        transact(session, models, newPac, pac2updatex, journals, transLogEntries, stock2update
                          , newStock, articles))
                          .mapBoth(e => RepositoryError(e.getMessage), _ => models.flatMap(_.lines).size + models.size)
      } yield nr

object PostTransactionRepositoryLive:
  val live: ZLayer[Resource[Task, Session[Task]], RepositoryError, PostTransactionRepository] =
      ZLayer.fromFunction(new PostTransactionRepositoryLive(_))



