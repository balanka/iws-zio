package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.TransactionModelId.{PAYABLES, PAYMENT, RECEIVABLES, SETTLEMENT}
import com.kabasoft.iws.domain.common.zeroAmount
import com.kabasoft.iws.domain.{Account, BankStatement, BusinessPartner, Company, Customer, FinancialsTransaction, FinancialsTransactionDetails, Supplier, Vat, common}
import com.kabasoft.iws.repository.{AccountRepository, BankStatementRepository, CompanyRepository, CustomerRepository, FinancialsTransactionRepository, SupplierRepository, VatRepository}
import zio.prelude.FlipOps
import zio.stream.*
import zio.*

import java.math.{BigDecimal, MathContext, RoundingMode}
import java.nio.file.{Files, Paths}
import java.time.Instant
final class BankStatementServiceLive(bankStmtRepo: BankStatementRepository
                                     , customerRepo: CustomerRepository
                                     , supplierRepo: SupplierRepository
                                     , companyRepo: CompanyRepository
                                     , accountRepo: AccountRepository
                                     , vatRepo: VatRepository
                                    ) extends BankStatementService:

  override def post(id: Long, companyId: String): ZIO[Any, RepositoryError, BankStatement] =
    postBankStmtCreateTransaction(List(id), companyId) *> bankStmtRepo.getById((id, BankStatement.MODELID, companyId))

  private def postBankStmtCreateTransaction(ids: List[Long], companyId: String): ZIO[Any, RepositoryError, Int] =
    for {
      accounts <- ZIO.logDebug(s"get account by modelid  ${companyId}  ") *> accountRepo.all((Account.MODELID, companyId))
      company <- ZIO.logDebug(s"get company by id  ${companyId}  ") *> companyRepo.getById((companyId, Company.MODEL_ID))
      bankStmt <- ZIO.logDebug(s"get bankStmt by ids  ${ids}  ") *>
        bankStmtRepo.getBy(ids, BankStatement.MODELID, companyId).map(_.toList)
      vat <- vatRepo.all((Vat.MODEL_ID, company.id))
      transactions <- ZIO.logDebug(s"Got bankStmt  ${bankStmt}  ") *> buildTransactions(bankStmt, vat, company, accounts)
      posted <- ZIO.logDebug(s"Created transactions  ${transactions}  ") *> bankStmtRepo.post(bankStmt, transactions.flatten)
      _ <- ZIO.logDebug(s"Transaction posted ${posted}  ")
    } yield posted

  override def post(ids: List[Long], companyId: String): ZIO[Any, RepositoryError, List[BankStatement]] =
    postBankStmtCreateTransaction(ids, companyId) *> bankStmtRepo.getBy(ids, BankStatement.MODELID, companyId).map(_.toList)

  private def buildTransactions(bs: List[BankStatement], vats: List[Vat], company: Company, accounts: List[Account]): 
                      ZIO[Any, RepositoryError, List[List[FinancialsTransaction]]] =
    bs.map(stmt =>
      (if (stmt.amount.compareTo(zeroAmount) >= 0) {
        customerRepo.getByIban(stmt.accountno, Customer.MODELID, stmt.company)
      } else {
        supplierRepo.getByIban(stmt.accountno, Supplier.MODELID, stmt.company)
      }).map(s => {
        List(buildPaymentSettlement(stmt, s, company, accounts), buildReceivablesPayables(stmt, s, vats.find(_.id == s.vatcode), accounts))
      })
    ).flip.mapError(e => RepositoryError(e.message))
  private def getX(optVat: Option[Vat], bs: BankStatement): Option[(String, BigDecimal)] =
  optVat.map(vat => {
    val vatAccount = if (bs.amount.compareTo(zeroAmount) >= 0) vat.outputVatAccount else vat.inputVatAccount
    val x = BigDecimal.valueOf(1).divide(BigDecimal.valueOf(1).add(vat.percent))
    val netAmount = bs.amount.multiply(x)
    (vatAccount, netAmount)
  }) //.mapError(e=>RepositoryError(e.getMessage))
  
  private def buildReceivablesPayables(bs: BankStatement
                                     , partner: BusinessPartner
                                     , optVat: Option[Vat]
                                     , accounts: List[Account]): FinancialsTransaction = {

  val modelid = if (bs.amount.compareTo(zeroAmount) >= 0) RECEIVABLES.id else PAYABLES.id

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

  def buildDetails(account: String
                   , accountName: String
                   , oaccount: String
                   , oaccountName: String
                   , amount: BigDecimal): FinancialsTransactionDetails = {
    def generate(account: String
                 , accountName: String
                 , oaccount: String
                 , oaccountName: String) =
      FinancialsTransactionDetails(-1L, -1L, account, side = true, oaccount, amount.abs(), bs.valuedate, bs.purpose, bs.currency, bs.company, accountName, oaccountName)

    if (bs.amount.compareTo(zeroAmount) < 0)
      generate(account, oaccount, accountName, oaccountName)
    //FinancialsTransactionDetails(-1L, -1L, account, side = true, oaccount, amount.abs(), bs.valuedate, bs.purpose, bs.currency, accountName, oaccountName)
    else generate(oaccount, account, oaccountName, accountName)
    //FinancialsTransactionDetails(-1L, -1L, oaccount, side = true, account, amount.abs(), bs.valuedate, bs.purpose, bs.currency, oaccountName, accountName)
  }
  buildTransaction(bs, partner, modelid, buildLines())
}
  private def buildPaymentSettlement(bs: BankStatement, partner: BusinessPartner, company: Company, accounts: List[Account]): FinancialsTransaction = {
  val bankAccountName = accounts.find(_.id == company.bankAcc).fold(s"Bank account with id ${company.bankAcc} not found!!!")(_.name)
  val accountName = accounts.find(_.id == partner.account).fold(s"Account with id ${partner.account} not found!!!")(_.name)
  val modelid = if (bs.amount.compareTo(zeroAmount) >= 0) SETTLEMENT.id else PAYMENT.id
  val line = if (modelid == PAYMENT.id) {
    FinancialsTransactionDetails(-1L, -1L, partner.account, side = true, company.bankAcc, bs.amount.abs(), bs.valuedate
      , bs.purpose, bs.currency, company.id, accountName, bankAccountName)
  } else {
    FinancialsTransactionDetails(-1L, -1L, company.bankAcc, side = true, partner.account, bs.amount.abs(), bs.valuedate
      , bs.purpose, bs.currency, company.id, bankAccountName, accountName)
  }
  buildTransaction(bs, partner, modelid, List(line))
}

  private def buildTransaction(bs: BankStatement, partner: BusinessPartner, modelid: Int, lines: List[FinancialsTransactionDetails]) =
    FinancialsTransaction(-1L, bs.id, -1L, "100", partner.account, bs.valuedate, Instant.now(), Instant.now(),
      common.getPeriod(bs.valuedate), posted = false, modelid, bs.company, bs.purpose, 0, 0, lines)

  override def importBankStmt(
                             path: String,
                             header: String,
                             char: String,
                             extension: String,
                             company: String,
                             buildFn: String => BankStatement = BankStatement.from
                           ): ZIO[Any, RepositoryError, Int] = {
  for {
    bs <- ZIO.logDebug(s"path ${path}") *>
      ZStream
        .fromJavaStream(Files.walk(Paths.get(path)))
        .filter(p => !Files.isDirectory(p) && p.toString.endsWith(extension))
        .flatMap { files =>
          ZStream
            .fromPath(files)
            .via(ZPipeline.utf8Decode >>> ZPipeline.splitLines)
            .tap(e => ZIO.logDebug(s"Element ${e}"))
            .filterNot(p => p.replaceAll(char, "").startsWith(header))
            //.map(p => buildFn(p))
            .map(p => buildFn(p.replaceAll(char, ""))) //.replaceAll("Spk ", "Spk")))
        }
        .mapError(e => RepositoryError(e.getMessage))
        .runCollect
        .map(_.toList)
    nr <- bankStmtRepo.create(bs).mapBoth(e => RepositoryError(e.message), list => list)
  } yield nr
}


object BankStatementServiceLive:
  val live: ZLayer[BankStatementRepository & FinancialsTransactionRepository & CustomerRepository & SupplierRepository &
    CompanyRepository & VatRepository & AccountRepository,
    RepositoryError,
    BankStatementService
  ] = ZLayer.fromFunction(new BankStatementServiceLive(_, _, _, _, _, _))

