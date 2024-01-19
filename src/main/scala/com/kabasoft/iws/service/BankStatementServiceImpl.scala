package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.common.zeroAmount
import com.kabasoft.iws.domain.{Account, BankStatement, BusinessPartner, Company, FinancialsTransaction, FinancialsTransactionDetails, Vat, common}
import com.kabasoft.iws.repository.{AccountRepository, BankStatementRepository, CompanyRepository, CustomerRepository, SupplierRepository, FinancialsTransactionRepository, VatRepository}
import zio.prelude.FlipOps
import zio.stream._
import zio._

import java.math.{BigDecimal, RoundingMode}
import java.nio.file.{Files, Paths}
import java.time.Instant
final class BankStatementServiceImpl( bankStmtRepo: BankStatementRepository,
                                      customerRepo: CustomerRepository,
                                      supplierRepo: SupplierRepository,
                                      companyRepo: CompanyRepository,
                                      accountRepo: AccountRepository,
                                      vatRepo: VatRepository
) extends BankStatementService {

  override def post(id: Long, companyId: String): ZIO[Any, RepositoryError, BankStatement] =
    postBankStmtCreateTransaction(List(id), companyId) *> bankStmtRepo.getById(id)

  private def postBankStmtCreateTransaction(ids: List[Long], companyId: String): ZIO[Any, RepositoryError, Int] =
    for {
      accounts <- ZIO.logInfo(s"get company by id  ${companyId}  ") *>accountRepo.all((Account.MODELID, companyId))
      company <- ZIO.logInfo(s"get company by id  ${companyId}  ") *> companyRepo.getBy(companyId)
      bankStmt <- ZIO.logInfo(s"get bankStmt by ids  ${ids}  ") *> bankStmtRepo.getById(ids).runCollect.map(_.toList)
      vat <- vatRepo.all((Vat.MODEL_ID, company.id))
      transactions <- ZIO.logInfo(s"Got bankStmt  ${bankStmt}  ") *> buildTransactions(bankStmt, vat, company, accounts)
      posted <- ZIO.logInfo(s"Created transactions  ${transactions}  ") *> bankStmtRepo.post(bankStmt, transactions.flatten)
      _ <- ZIO.logInfo(s"Transaction posted ${posted}  ")
    } yield posted

  override def post(ids: List[Long], companyId: String): ZIO[Any, RepositoryError, List[BankStatement]] = {
    postBankStmtCreateTransaction(ids, companyId) *> bankStmtRepo.getById(ids).runCollect.map(_.toList)
  }

  private def buildTransactions(bs: List[BankStatement], vats:List[Vat], company: Company, accounts:List[Account]): ZIO[Any, RepositoryError, List[List[FinancialsTransaction]]] = bs.map(stmt =>
    (if (stmt.amount.compareTo(zeroAmount) >= 0) {
      customerRepo.getByIban(stmt.accountno, stmt.company)
    } else {
      supplierRepo.getByIban(stmt.accountno, stmt.company)
    }).map(s => {
      List(buildPaymentSettlement(stmt, s, company, accounts), buildReceivablesPayables(stmt, s, vats.find(_.id == s.vatcode), accounts))
    })
  ).flip
  private def  getX (optVat:Option[Vat], bs: BankStatement): Option[(String, BigDecimal)] =
    optVat.map(vat => {
    val vatAccount = if (bs.amount.compareTo(zeroAmount) >= 0) vat.outputVatAccount  else vat.inputVatAccount
      val x = (1/(1+vat.percent.floatValue())).toString
      val netAmount = bs.amount.multiply(new BigDecimal(x).setScale(6, RoundingMode.HALF_UP))
          .setScale(2, RoundingMode.HALF_UP)
     (vatAccount, netAmount)
  })
  private def buildReceivablesPayables(bs: BankStatement, partner: BusinessPartner, optVat: Option[Vat], accounts:List[Account]): FinancialsTransaction = {

    val modelid = if (bs.amount.compareTo(zeroAmount) >= 0) 122 else 112

    def buildLines(): List[FinancialsTransactionDetails] = {
      val emptyLines = List.empty[FinancialsTransactionDetails]
      val lines: List[FinancialsTransactionDetails] = getX(optVat, bs).map(vatAccountAndNetAmount => {
        val vatAccount = vatAccountAndNetAmount._1
        val netAmount = vatAccountAndNetAmount._2
        val vatAmount = bs.amount.abs().subtract(netAmount.abs()).setScale(2, RoundingMode.HALF_UP)
        val oaccountName = accounts.find(_.id == partner.oaccount).fold(s"OAccount with id ${partner.oaccount} not found!!!")(_.name)
        val accountName = accounts.find(_.id == partner.account).fold(s"Account with id ${partner.account} not found!!!")(_.name)
        val vatAccountName = accounts.find(_.id == vatAccount).fold(s"Account with id ${partner.account} not found!!!")(_.name)
        val netLine = buildDetails(partner.oaccount, oaccountName, partner.account, accountName, netAmount)
        if (vatAmount.abs().compareTo(zeroAmount) > 0) {
          val vatLine = buildDetails(vatAccount, vatAccountName, partner.account, accountName, vatAmount)
          List(netLine, vatLine)
        } else List(netLine)
      }).getOrElse(emptyLines)
      lines
    }

    def buildDetails(account: String, accountName:String, oaccount: String, oaccountName:String, amount: BigDecimal): FinancialsTransactionDetails =
      if (bs.amount.compareTo(zeroAmount) < 0) {
        FinancialsTransactionDetails(-1L, -1L, account, true, oaccount, amount.abs(), bs.valuedate, bs.purpose, bs.currency, accountName, oaccountName)
      } else {
        FinancialsTransactionDetails(-1L, -1L, oaccount, true, account, amount.abs(), bs.valuedate, bs.purpose, bs.currency, oaccountName, accountName)
      }

    buildTransaction(bs, partner, modelid, buildLines())

  }
  private[this] def buildPaymentSettlement(bs: BankStatement, partner: BusinessPartner, company: Company, accounts:List[Account]): FinancialsTransaction = {
    val bankAccountName = accounts.find(_.id == company.bankAcc).fold(s"Bank account with id ${company.bankAcc} not found!!!")(_.name)
    val accountName = accounts.find(_.id == partner.account).fold(s"Account with id ${partner.account} not found!!!")(_.name)
    val modelid = if (bs.amount.compareTo(zeroAmount) >= 0) 124 else 114
    val line = if(modelid ==114) {
      FinancialsTransactionDetails(-1L, -1L, partner.account , true, company.bankAcc, bs.amount.abs(), bs.valuedate, bs.purpose, bs.currency, accountName, bankAccountName)
    } else {
      FinancialsTransactionDetails(-1L, -1L, company.bankAcc,  true, partner.account, bs.amount.abs(), bs.valuedate, bs.purpose, bs.currency, bankAccountName, accountName)
    }
    buildTransaction(bs, partner, modelid, List(line))
  }

  private def buildTransaction(bs: BankStatement, partner: BusinessPartner, modelid:Int, lines: List[FinancialsTransactionDetails]) = {
    val date = Instant.now()
    val period = common.getPeriod(bs.valuedate)
    FinancialsTransaction(
      -1L,
      bs.id,
      -1L,
      "100",
      partner.account,
      bs.valuedate,
      date,
      date,
      period,
      false,
      modelid,
      bs.company,
      bs.purpose,
      0,
      0,
      lines
    )
  }

  override def importBankStmt(
                               path: String,
                               header: String,
                               char: String,
                               extension: String,
                               company: String,
                               buildFn: String => BankStatement = BankStatement.from
                             ): ZIO[Any, RepositoryError, Int] = {
    for {
      bs <- ZStream
        .fromJavaStream(Files.walk(Paths.get(path)))
        .filter(p => !Files.isDirectory(p) && p.toString.endsWith(extension))
        .flatMap { files =>
          ZStream
            .fromPath(files)
//           .via(ZPipeline.utf8Decode)
//            .map(_.replaceAll("ü", "ue")
//              .replaceAll("Ü", "Ue")
//              .replaceAll("ö", "oe")
//              .replaceAll("Ö", "Oe"))
//            .via (ZPipeline.splitLines)
            .via(ZPipeline.utf8Decode >>> ZPipeline.splitLines)
            .tap(e => ZIO.logInfo(s"Element ${e}"))
            .filterNot(p => p.replaceAll(char, "").startsWith(header))
            //.map(p => buildFn(p))
             .map(p => buildFn(p.replaceAll(char, "")))//.replaceAll("Spk ", "Spk")))
        }
        .mapError(e => RepositoryError(e.getMessage))
        .runCollect
        .map(_.toList)
      nr <-    bankStmtRepo.create2(bs)
    } yield nr
  }
}

object BankStatementServiceImpl {
  val live: ZLayer[
    BankStatementRepository with FinancialsTransactionRepository with CustomerRepository with SupplierRepository with CompanyRepository with VatRepository with AccountRepository,
    RepositoryError,
    BankStatementService
  ] = ZLayer.fromFunction(new BankStatementServiceImpl(_, _, _, _, _, _))
}
