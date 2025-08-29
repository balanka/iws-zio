package com.kabasoft.iws.repository

import cats.effect.Resource
import skunk.*
import zio.interop.catz.asyncInstance
import zio.{Task, UIO, ZIO, ZLayer}
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{FinancialsTransaction, FinancialsTransactionDetails, Journal, PeriodicAccountBalance}

final case class PostFinancialsTransactionRepositoryLive(postgres: Resource[Task, Session[Task]]) extends
                PostFinancialsTransactionRepository, MasterfileCRUD:
  
  def delete(p:(Long, Int, String)): ZIO[Any, RepositoryError, Int] =
    executeWithTx(postgres, p, FinancialsTransactionRepositorySQL.DELETE, 1)

 
  
  def transact(s: Session[Task], models2Insert: List[FinancialsTransaction]
               , models2Update: List[FinancialsTransaction], pac2Insert: List[PeriodicAccountBalance]
               , pac2update: List[PeriodicAccountBalance], journals: List[Journal]): Task[Unit] =
    s.transaction.use: xa =>
      s.prepareR(PacRepositorySQL.insert).use: pciPac =>
        s.prepareR(PacRepositorySQL.UPDATE).use: pcuPac =>
          s.prepareR(FinancialsTransactionRepositorySQL.insert).use: pciFTr =>
            s.prepareR(FinancialsTransactionRepositorySQL.insertDetails).use: pciLFTr =>
              s.prepareR(FinancialsTransactionRepositorySQL.updatePosted).use: pcuFTr =>
                s.prepareR(FinancialsTransactionRepositorySQL.UPDATE_DETAILS).use: pcuLFTr =>
                  s.prepareR(JournalRepositorySQL.insert).use: pciJour =>
                    tryExec4(xa, pciPac,  pcuPac, pciFTr, pciLFTr, pcuFTr, pcuLFTr, pciJour
                      , pac2Insert, pac2update.map(PeriodicAccountBalance.encodeIt2)
                      , models2Insert, models2Insert.flatMap(_.lines).map(FinancialsTransactionDetails.encodeIt4)
                      , models2Update.map(FinancialsTransaction.encodeIt3), List.empty[FinancialsTransactionDetails.TYPE2]
                      , journals)

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
                                  transact(session, models2Insert.map(buildId), List.empty[FinancialsTransaction]
                                    , pac2Insert, pac2updatex, journals))
                              .mapBoth(e => RepositoryError(e.getMessage), _ => models2Insert.flatMap(_.lines).size
                                + models2Insert.size +pac2Insert.size+pac2updatex.size+journals.size)
     } yield nr

object PostFinancialsTransactionRepositoryLive:
    val live: ZLayer[Resource[Task, Session[Task]], Throwable, PostFinancialsTransactionRepository] =
      ZLayer.fromFunction(new PostFinancialsTransactionRepositoryLive(_))
