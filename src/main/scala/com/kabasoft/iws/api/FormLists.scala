package com.kabasoft.iws.api

import com.kabasoft.iws.domain._
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.repository._
import zio._

/**
 * Single source of truth for form list data.
 *
 * Two loaders:
 *   - loadBase(company, parentModelId)              → accounts, stores, contacts, fmodules (filtered by parent)
 *   - loadWithArticlesAndVats(company, parentModelId) → same + articles, vats
 *
 * Per-request memoization via FiberRef keyed on (company, parentModelId).
 * Same key requested twice in one request → one DB hit.
 */
object FormLists:

  final case class FormData(
                             accounts: List[Account],
                             stores:   List[Store],
                             contacts: List[Contact],
                             fmodules: List[Fmodule],
                             articles: List[Article],
                             vats:     List[Vat]
                           )

  type BaseEnv =
    AccountRepository & FModuleRepository & PartnerRepository & StoreRepository

  type WithArticlesAndVats =
    BaseEnv & ArticleRepository & VatRepository

  type Cache = FiberRef[Map[(String, Int), FormData]]

  val cacheLayer: ULayer[Cache] =
    ZLayer.scoped(FiberRef.make(Map.empty[(String, Int), FormData]))

  /** Base lists: accounts, stores, contacts, fmodules filtered by parent. */
  def loadBase(
                company: String,
                parentModelId: Int
              ): ZIO[BaseEnv & Cache, RepositoryError, FormData] =
    val k = (company, parentModelId)
    for {
      cache <- ZIO.service[Cache]
      hit   <- cache.get.map(_.get(k))
      data  <- hit match
        case Some(d) => ZIO.succeed(d)
        case None =>
          for {
            d <- loadBaseFresh(company, parentModelId)
            _ <- cache.update(_ + (k -> d))
          } yield d
    } yield data

  /** Like `loadBase`, but fetches articles and vats as well. */
  def loadWithArticlesAndVats(
                               company: String,
                               parentModelId: Int
                             ): ZIO[WithArticlesAndVats & Cache, RepositoryError, FormData] =
    for {
      base     <- loadBase(company, parentModelId)   // reuses the cache
      articles <- ArticleRepository.all((ModelId.ARTICLE.modelid, company))
      vats     <- VatRepository.all((ModelId.VAT.modelid, company))
    } yield base.copy(articles = articles, vats = vats)

  private def loadBaseFresh(
                             company: String,
                             parentModelId: Int
                           ): ZIO[BaseEnv, RepositoryError, FormData] =
    for {
      accs     <- AccountRepository.all((ModelId.ACCOUNT.modelid, company))
      stores   <- StoreRepository.all((ModelId.STORE.modelid, company))
      contacts <- PartnerRepository.all((ModelId.CONTACT.modelid, company))
      fmodules <- FModuleRepository.getByParent(
        ModelId.FMODULE.modelid,
        parentModelId.toString,
        company)
    } yield FormData(
      accounts = accs,
      stores   = stores,
      contacts = contacts,
      fmodules = fmodules,
      articles = Nil,
      vats     = Nil)