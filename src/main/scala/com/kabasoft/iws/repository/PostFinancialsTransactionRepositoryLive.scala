package com.kabasoft.iws.repository

import cats.effect.Resource
import cats.syntax.all.*
import cats.*
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*
import zio.prelude.{FlipOps, Identity}
import zio.stream.interop.fs2z.*
import zio.{Task, UIO, ZIO, ZLayer}
import com.kabasoft.iws.domain.AppError.RepositoryError
import MasterfileCRUD.{IwsCommand, IwsCommandLP}
import com.kabasoft.iws.domain.{FinancialsTransaction, Journal, PeriodicAccountBalance}

final case class PostFinancialsTransactionRepositoryLive(postgres: Resource[Task, Session[Task]]) extends
                PostFinancialsTransactionRepository, MasterfileCRUD:

  private def createPacs4T(models: List[PeriodicAccountBalance]): ZIO[Any, RepositoryError, Int] =
    val cmd = PacRepositorySQL.insertAll(models.length)
    val cmds = IwsCommandLP(models, PeriodicAccountBalance.encodeIt, cmd)
    executeBatchWithTx(postgres, List.empty, List(cmds))
    ZIO.succeed(models.size)
   // executeWithTx(postgres, models.map(PeriodicAccountBalance.encodeIt), PacRepositorySQL.insertAll(models.size), models.size)

  private def createJ4T(models: List[Journal]): ZIO[Any, RepositoryError, Int] =
    val cmd = JournalRepositorySQL.insertAll(models.length)
    val cmds = IwsCommandLP(models, Journal.encodeIt, cmd)
    executeBatchWithTx(postgres, List.empty, List(cmds))
    ZIO.succeed(models.size)
    //executeWithTx(postgres, models.map(Journal.encodeIt), JournalRepositorySQL.insertAll(models.size), models.size)

  private def modifyPacs4T(models: List[PeriodicAccountBalance]): ZIO[Any, RepositoryError, Int] =
    execPreparedCommand(postgres, models, PeriodicAccountBalance.encodeIt2, List(PacRepositorySQL.UPDATE))
    //executeBatchWithTx(postgres, List.empty, List(cmds))
    //ZIO.succeed(models.size)
  
    //executeBatchWithTxK(postgres, models , PacRepositorySQL.UPDATE, PeriodicAccountBalance.encodeIt2)

  def delete(p:(Long, Int, String)): ZIO[Any, RepositoryError, Int] =
    executeWithTx(postgres, p, FinancialsTransactionRepositorySQL.DELETE, 1)

  //    val cmds = models.map(model => IwsCommand(model, FinancialsTransaction.encodeIt2, FinancialsTransactionRepositorySQL.updatePosted))
  //    executeBatchWithTx(postgres, cmds, List.empty)
  //    ZIO.succeed(models.size)

  // executeWithTx(postgres, models.map(PeriodicAccountBalance.encodeIt), PacRepositorySQL.insertAll(models.size), models.size)
  // executeBatchWithTxK(postgres, models, PacRepositorySQL.UPDATE, PeriodicAccountBalance.encodeIt2)
  //executeWithTx(postgres, models.map(Journal.encodeIt), JournalRepositorySQL.insertAll(models.size), models.size)

  override def post(models: List[FinancialsTransaction], pac2Insert: List[PeriodicAccountBalance], pac2update: UIO[List[PeriodicAccountBalance]],
                    journals: List[Journal]): ZIO[Any, RepositoryError, Int] = for {
    pac2updatex <- pac2update
    _ <- ZIO.logInfo(s" New Pacs  to insert into DB ${pac2Insert}")
    _ <- ZIO.logInfo(s" Old Pacs  to update in DB ${pac2updatex}")
    _ <- ZIO.logInfo(s" journals  ${journals}")
    _ <- ZIO.logInfo(s" Transaction posted  ${models}")
    z = ZIO.when(models.nonEmpty)(updatePostedField4T(models))
      .zipWith(ZIO.when(pac2Insert.nonEmpty)(createPacs4T(pac2Insert)))((i1, i2) => i1.getOrElse(0) + i2.getOrElse(0))
      .zipWith(ZIO.when(pac2updatex.nonEmpty)(modifyPacs4T(pac2updatex)))((i1, i2) => i1 + i2.getOrElse(0))
      .zipWith(ZIO.when(journals.nonEmpty)(createJ4T(journals)))((i1, i2) => i1 + i2.getOrElse(0))
    nr <- z.mapError(e => {
      ZIO.logDebug(s" Error >>>>>>>  ${e.toString}")
      RepositoryError(e.toString)
    })
  } yield nr

  private def updatePostedField4T(models: List[FinancialsTransaction]): ZIO[Any, Exception, Int] =
    val cmds = models.map(model => IwsCommand(model, FinancialsTransaction.encodeIt2, FinancialsTransactionRepositorySQL.updatePosted))
    executeBatchWithTx(postgres, cmds, List.empty)
    ZIO.succeed(models.size)

object PostFinancialsTransactionRepositoryLive:
    val live: ZLayer[Resource[Task, Session[Task]], Throwable, PostFinancialsTransactionRepository] =
      ZLayer.fromFunction(new PostFinancialsTransactionRepositoryLive(_))
