package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain._
import com.kabasoft.iws.domain.common.zeroAmount
import com.kabasoft.iws.repository._
import zio._

import java.time.Instant

final class AssetsServiceLive(assetRepo: AssetRepository
                              , accountRepo: AccountRepository
                              , companyRepo: CompanyRepository,
                              ftrRepo: FinancialsTransactionRepository) extends AssetsService:

  override def generate( period:Int, company: String): ZIO[Any, RepositoryError, Int] = for {
    _<- ZIO.logInfo(s" Generating  transaction for the period  ${period} and   company ${company}")
    transactions <- build(company).debug("transactions")
    nr           <-  ZIO.succeed(transactions).map(_.size) 
  }yield nr

  private def build(companyId: String): ZIO[Any, RepositoryError, List[FinancialsTransaction]] = for {
    //<- ZIO.logInfo(s" Posting transaction with id ${id} of company ${company}")
    company  <- companyRepo.getById((companyId, Company.MODEL_ID))//.debug("employee")
    assets   <- assetRepo.all((Asset.MODELID, companyId))//.debug("assets")
    accounts <- accountRepo.all((Account.MODELID, companyId))//.debug("accounts")

  } yield buildTransaction(assets, accounts, company)


  private def buildTransactionDetails(asset:Asset, accounts:List[Account], company: Company) = 
     val amount = zeroAmount // ToDo replace this implement the calculation of the amount to depreciate
     FinancialsTransactionDetails(-1L, -1L, asset.oaccount, side = true, asset.account, amount,
      Instant.now(), asset.name, company.currency, getName(accounts, asset.oaccount), getName(accounts, asset.account))
     
  def getName (accounts:List[Account], id:String): String =
    accounts.find(_.id == id).fold(s"Account with id ${id} not found!!!")(_.name)

  private def buildTransaction(assets:List[Asset],  accounts:List[Account], company: Company) = assets.map(asset=>
    val date = Instant.now()
    val period = common.getPeriod(date)
    val line: FinancialsTransactionDetails = buildTransactionDetails (asset, accounts, company)
    FinancialsTransaction(-1L, -1L, -1L, "100", asset.oaccount, date, date, date, period, posted = false, TransactionModelId.GENERAL_LEDGER.id,
      asset.company, "Depreciation of asset "+period, 0, 0, List(line))
  )//.mapBoth(e => RepositoryError(e.getMessage), a => a)

object AssetsServiceLive:
  val live: ZLayer[AssetRepository& AccountRepository& FinancialsTransactionRepository& CompanyRepository,
                       RepositoryError, AssetsService] =
    ZLayer.fromFunction(new AssetsServiceLive(_, _, _,  _))


