package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.*
import com.kabasoft.iws.domain.{AppError, TransactionLog}
import com.kabasoft.iws.repository.Schema.{authenticationErrorSchema, transactionLogSchema, repositoryErrorSchema}
import com.kabasoft.iws.repository.TransactionLogRepository
import zio.*
import zio.http.*
import zio.http.codec.*
import zio.http.codec.PathCodec.{int, path, string}
import zio.http.endpoint.Endpoint
import zio.schema.Schema

object InventoryJournalEndpoint:
  val modelidDoc = "The modelId for identifying the typ of transaction "
  val periodDoc = "The period for selecting the inventory journal entries"
  val fromPeriodDoc = "The period to start selecting the inventory journal entries from"
  val tpPeriodDoc = "The period to stop selecting the inventory journal entries to"
  val accountIdDoc = "The account id for which to select the inventory journal entries"
  val artIdDoc = "The id of the article which to select the inventory journal entries for"
  val storeIdDoc = "The id of the store which to select the inventory journal entries for"
  val fromDoc = "The starting period for selecting the inventory journal entries"
  val toDoc = "The end period for selecting the inventory journal entries"
  val companyDoc = "The company whom the store belongs to (i.e. 111111)"
  val mByPeriodDoc = "Get inventory journal entries per period and company"
  val mByAccountFromToPeriodDoc = "Get inventory journal entries for an account and within  period  from/to and company"
  ///journal/5000/202507/202507
  
  private val find4Period = Endpoint(RoutePattern.GET / "ijournal" / string("company") ?? Doc.p(companyDoc)
    / int("fromPeriod") ?? Doc.p(fromPeriodDoc) / int("toPeriod") ?? Doc.p(tpPeriodDoc)
    ).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[List[TransactionLog]] ?? Doc.p(mByPeriodDoc)
  
  private val find4StorePeriod = Endpoint(RoutePattern.GET / "ijournal" / string("company")?? Doc.p(companyDoc)
    /string("storeId")?? Doc.p(storeIdDoc) / int("from")?? Doc.p(fromDoc) / int("to")?? Doc.p(toDoc)
  ).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[List[TransactionLog]] ?? Doc.p(mByAccountFromToPeriodDoc)
  
  private val find4ArticlePeriod = Endpoint(RoutePattern.GET / "ijournal" / string("company")?? Doc.p(companyDoc)
    /string("artId")?? Doc.p(artIdDoc) / int("from")?? Doc.p(fromDoc) / int("to")?? Doc.p(toDoc)
  ).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[List[TransactionLog]] ?? Doc.p(mByAccountFromToPeriodDoc)
  
  private val find4StoreArticlePeriod = Endpoint(RoutePattern.GET / "ijournal" / string("company")?? Doc.p(companyDoc)
    /string("storeId")?? Doc.p(storeIdDoc) /string("artId")?? Doc.p(artIdDoc)
    / int("from")?? Doc.p(fromDoc) / int("to")?? Doc.p(toDoc)
    ).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[List[TransactionLog]] ?? Doc.p(mByAccountFromToPeriodDoc)


  val find4PeriodRoute =
    find4Period.implement(p => for {
      _ <- ZIO.logInfo(s"Get entries 4 period from ${p._2}, to ${p._3} and  company ${p._1}")
      journalEntries4Account <- TransactionLogRepository.find4Period(p._2, p._3, p._1)
    } yield journalEntries4Account
    )

  val find4ArticlePeriodToRoute =
    find4ArticlePeriod.implement(p => for {
       _ <- ZIO.logInfo(s"Get entries 4 article id  ${p._2}, from ${p._3}, to ${p._4} and  company ${p._1}")
       journalEntries4Account <- TransactionLogRepository.find4ArticlePeriod(p._2, p._3, p._4, p._1)
     } yield journalEntries4Account
    )

  val find4StorePeriodRoute =
    find4StorePeriod.implement (p =>  for {
    _<- ZIO.logInfo(s"Get entries 4 store id  ${p._2}, from ${p._3}, to ${p._4} and  company ${p._1}")
    journalEntries4Account <- TransactionLogRepository.find4StorePeriod(p._2, p._3, p._4, p._1)
  } yield journalEntries4Account
  )
  val find4StoreArticlePeriodRoute =
    find4StoreArticlePeriod.implement(p => for {
      _ <- ZIO.logInfo(s"Get entries 4 store  ${p._2}, article id  ${p._3}, from ${p._4}, to ${p._5} and  company ${p._1}")
      journalEntries4Account <- TransactionLogRepository.find4StoreArticlePeriod(p._2, p._3, p._4, p._5, p._1)
    } yield journalEntries4Account
    )
  
  val inventoryJournalRoutes = Routes(find4PeriodRoute, find4ArticlePeriodToRoute, find4StorePeriodRoute, find4StoreArticlePeriodRoute) @@ Middleware.debug


