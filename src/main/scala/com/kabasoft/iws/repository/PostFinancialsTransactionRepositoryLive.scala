package com.kabasoft.iws.repository

import cats.effect.Resource
import cats.syntax.all.*
import cats.*
import skunk.*
import skunk.codec.all.*
import cats.syntax.all.*
import skunk.implicits.*
import zio.prelude.{FlipOps, Identity}
import zio.stream.interop.fs2z.*
import zio.interop.catz.*
import zio.{Task, UIO, ZIO, ZLayer}
import com.kabasoft.iws.domain.AppError.RepositoryError
import MasterfileCRUD.{UpdateCommand, InsertBatch}
import com.kabasoft.iws.domain.{FinancialsTransaction, Journal, PeriodicAccountBalance}

final case class PostFinancialsTransactionRepositoryLive(postgres: Resource[Task, Session[Task]]) extends
                PostFinancialsTransactionRepository, MasterfileCRUD:
  
  def delete(p:(Long, Int, String)): ZIO[Any, RepositoryError, Int] =
    executeWithTx(postgres, p, FinancialsTransactionRepositorySQL.DELETE, 1)
  
  def transact(s: Session[Task], models: List[FinancialsTransaction], pac2Insert: List[PeriodicAccountBalance]
               , pac2update: List[PeriodicAccountBalance], journals: List[Journal]): Task[Unit] =
    s.transaction.use: xa =>
      s.prepareR(PacRepositorySQL.insert).use: pciPac =>
        s.prepareR(PacRepositorySQL.UPDATE).use: pcuPac =>
          s.prepareR(JournalRepositorySQL.insert).use: pciJour =>
            s.prepareR(FinancialsTransactionRepositorySQL.updatePosted).use: pcuFTr =>
              tryExec2(xa, pciPac, pciJour, pcuPac, pcuFTr, pac2Insert, journals
                , pac2update.map(PeriodicAccountBalance.encodeIt2), models.map(FinancialsTransaction.encodeIt3))

  override def post(models: List[FinancialsTransaction], pac2Insert: List[PeriodicAccountBalance], pac2update: UIO[List[PeriodicAccountBalance]],
                    journals: List[Journal]): ZIO[Any, RepositoryError, Int] = 
      for {
          pac2updatex <- pac2update
                    _ <- ZIO.logInfo(s" New Pacs  to insert into DB ${pac2Insert}")
                    _ <- ZIO.logInfo(s" Old Pacs  to update in DB ${pac2updatex}")
                    _ <- ZIO.logInfo(s" journals  ${journals}")
                    _ <- ZIO.logInfo(s" Transaction posted  ${models}")
                    nr <-   (postgres
                              .use:
                                  session =>
                                  transact(session, models, pac2Insert, pac2updatex, journals))
                              .mapBoth(e => RepositoryError(e.getMessage), _ => models.flatMap(_.lines).size + models.size)
     } yield nr

object PostFinancialsTransactionRepositoryLive:
    val live: ZLayer[Resource[Task, Session[Task]], Throwable, PostFinancialsTransactionRepository] =
      ZLayer.fromFunction(new PostFinancialsTransactionRepositoryLive(_))
