package com.kabasoft.iws.repository
import cats.effect.Resource
import cats.syntax.all.*
import cats._
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*
import zio.prelude.FlipOps
import zio.stream.interop.fs2z.*
import zio.{Task, ZIO, UIO, ZLayer}
import com.kabasoft.iws.domain.{Article,  Masterfile, PeriodicAccountBalance, Journal, Stock, Transaction, TransactionLog }
import com.kabasoft.iws.domain.AppError.RepositoryError
import MasterfileCRUD.{IwsCommand, IwsCommandLP}
import java.time.{Instant, LocalDateTime, ZoneId}

final case class PostTransactionRepositoryLive(postgres: Resource[Task, Session[Task]]) extends PostTransactionRepository, MasterfileCRUD:
  
  private def createPacs4T(models : List[PeriodicAccountBalance]): ZIO[Any, Exception, Int] =
    val cmd = PacRepositorySQL.insertAll(models.length)
    val cmds = IwsCommandLP(models, PeriodicAccountBalance.encodeIt, cmd)
    executeBatchWithTx(postgres, List.empty, List(cmds))
    ZIO.succeed(models.size)


//  private def createT[A, B, C, D](models: List[A], commands: List[IwsCommand[A,B]]
//                            , commandLPs: List[IwsCommandLP[C, D]]): ZIO[Any, Exception, Int] =
//    executeBatchWithTx(postgres, commands, commandLPs)
//    ZIO.succeed(models.size)
    
  private def createLog4T(models : List[TransactionLog]): ZIO[Any, Exception, Int] =
    val cmd = TransactionLogRepositorySQL.insertAll(models.length)
    val cmds = IwsCommandLP(models, TransactionLog.encodeIt, cmd)
    //createT(models, List.empty, cmds)
    executeBatchWithTx(postgres, List.empty, List(cmds))
    ZIO.succeed(models.size)
  
  private def createStock4T(models : List[Stock]): ZIO[Any, Exception, Int] =
    val cmd = StockRepositorySQL.insertAll(models.length)
    val cmds = IwsCommandLP(models, Stock.encodeIt, cmd)
    executeBatchWithTx(postgres, List.empty, List(cmds))
    ZIO.succeed(models.size)

  private def createJ4T(journals: List[Journal]): ZIO[Any, Exception, Int] =
   val cmd = JournalRepositorySQL.insertAll(journals.length)
   val cmds = IwsCommandLP(journals, Journal.encodeIt, cmd)
   executeBatchWithTx(postgres, List.empty, List(cmds))
   ZIO.succeed(journals.size)


  private def modifyPacs4T(models: List[PeriodicAccountBalance]) =
    val cmds = models.map(model=>IwsCommand(model, PeriodicAccountBalance.encodeIt2, PacRepositorySQL.update))
    executeBatchWithTx(postgres, cmds, List.empty)
    ZIO.succeed(models.size)


  private def modifyStock4T(models: List[Stock]) =
    val cmds = models.map(model=>IwsCommand(model, Stock.encodeIt2, StockRepositorySQL.updateQuantity))
     executeBatchWithTx(postgres, cmds, List.empty)
     ZIO.succeed(models.size)


  private def modifyPrices4T(models: List[Article]) =
    val cmds = models.map(model=>IwsCommand(model, Article.encodeIt2, ArticleRepositorySQL.updatePrices))
    executeBatchWithTx(postgres, cmds, List.empty)
    ZIO.succeed(models.size)

  override def post(models: List[Transaction], pac2Insert: List[PeriodicAccountBalance], pac2update: UIO[List[PeriodicAccountBalance]], transLogEntries: List[TransactionLog],
                      journals: List[Journal], stocks: List[Stock], newStock: List[Stock], articles: List[Article]): ZIO[Any, RepositoryError, Int] = for {
      pac2updatex <- pac2update
      _ <- ZIO.when(pac2Insert.nonEmpty)(ZIO.logInfo(s" New Pacs  to insert into DB ${pac2Insert}"))
      _ <- ZIO.when(pac2updatex.nonEmpty)(ZIO.logInfo(s" Old Pacs  to update in DB ${pac2updatex}"))
      _ <- ZIO.when(transLogEntries.nonEmpty)(ZIO.logInfo(s" Transaction log  ${transLogEntries}"))
      _ <- ZIO.when(journals.nonEmpty)(ZIO.logInfo(s" journals  ${journals}"))
      _ <- ZIO.logInfo(s" Transaction posted  ${models}")
      z = ZIO.when(models.nonEmpty)(updatePostedField4T(models))
        .zipWith(ZIO.when(pac2Insert.nonEmpty)(createPacs4T(pac2Insert)))((i1, i2) => i1.getOrElse(0) + i2.getOrElse(0))
        .zipWith(ZIO.when(pac2updatex.nonEmpty)(modifyPacs4T(pac2updatex)))((i1, i2) => i1 + i2.getOrElse(0))
        .zipWith(ZIO.when(transLogEntries.nonEmpty)(createLog4T(transLogEntries)))((i1, i2) => i1 + i2.getOrElse(0))
        .zipWith(ZIO.when(journals.nonEmpty)(createJ4T(journals)))((i1, i2) => i1 + i2.getOrElse(0))
        .zipWith(ZIO.when(newStock.nonEmpty)(createStock4T(newStock)))((i1, i2) => i1 + i2.getOrElse(0))
        .zipWith(ZIO.when(stocks.nonEmpty)(modifyStock4T(stocks)))((i1, i2) => i1 + i2.getOrElse(0))
        .zipWith(ZIO.when(articles.nonEmpty)(modifyPrices4T(articles)))((i1, i2) => i1 + i2.getOrElse(0))

      nr <- z.mapError(e => {
        ZIO.logDebug(s" Error >>>>>>>  ${e.getMessage}")
        RepositoryError(e.getMessage)
      })//.provideLayer(driverLayer)
    } yield nr

  private def updatePostedField4T(models: List[Transaction]): ZIO[Any, Exception, Int] = 
    val cmds = models.map(model => IwsCommand(model, Transaction.encodeIt2, TransactionRepositorySQL.updatePosted))
    executeBatchWithTx(postgres, cmds, List.empty)
    ZIO.succeed(models.size)


object PostTransactionRepositoryLive:
  val live: ZLayer[Resource[Task, Session[Task]], RepositoryError, PostTransactionRepository] =
      ZLayer.fromFunction(new PostTransactionRepositoryLive(_))



