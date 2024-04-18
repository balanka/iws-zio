package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain._
import com.kabasoft.iws.domain.common.zeroAmount
import com.kabasoft.iws.repository._
import zio._

import java.time.Instant

final class AssetsServiceImpl (assetRepo: AssetRepository,  accountRepo: AccountRepository, companyRepo: CompanyRepository,
                                  ftrRepo: FinancialsTransactionRepository) extends AssetsService {

  override def generate( period:Int, company: String): ZIO[Any, RepositoryError, Int] = for {
    _<- ZIO.logInfo(s" Generating  transaction for the period  ${period} and   company ${company}")
    transactions <- build(company).debug("transactions")
    nr<-  ZIO.succeed(transactions).map(_.size) //ftrRepo.create(transactions).map(_.size)
  }yield nr

  private def build(companyId: String): ZIO[Any, RepositoryError, List[FinancialsTransaction]] = for {
    //<- ZIO.logInfo(s" Posting transaction with id ${id} of company ${company}")
    company <- companyRepo.getBy( companyId).debug("employee")
    assets <- assetRepo.all((Asset.MODELID, companyId)).debug("assets")
    accounts<- accountRepo.all((Account.MODELID, companyId)).debug("accounts")

  } yield assets.map(asset => buildTransaction(asset, List(buildTransactionDetails (asset, accounts, company) )))


  private def buildTransactionDetails(asset:Asset, accounts:List[Account], company: Company) = {
     val amount = zeroAmount // ToDo replace this implement the calculation of the amount to depreciate
     FinancialsTransactionDetails(-1L, -1L, asset.oaccount, side = true, asset.account, amount,
      Instant.now(), asset.name, company.currency, getName(accounts, asset.oaccount), getName(accounts, asset.account))
  }
  def getName (accounts:List[Account], id:String): String =
    accounts.find(_.id == id).fold(s"Account with id ${id} not found!!!")(_.name)

  private def buildTransaction(ass:Asset, lines: List[FinancialsTransactionDetails]) = {
    val date = Instant.now()
    val period = common.getPeriod(date)
    FinancialsTransaction(
      -1L,
      -1L,
      -1L,
      "100",
      ass.oaccount,
      date,
      date,
      date,
      period,
      posted = false,
      TransactionModelId.GENERAL_LEDGER.id,
      ass.company,
      "Depreciation of asset "+period,
      0,
      0,
      lines
    )
  }
}
object AssetsServiceImpl {
  val live: ZLayer[AssetRepository  with AccountRepository with FinancialsTransactionRepository with CompanyRepository,
                       RepositoryError, AssetsService] =
    ZLayer.fromFunction(new AssetsServiceImpl(_, _, _,  _))
}

