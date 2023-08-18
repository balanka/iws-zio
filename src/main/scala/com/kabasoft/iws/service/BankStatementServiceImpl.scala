package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.common.zeroAmount
import com.kabasoft.iws.domain.{BankStatement, BusinessPartner, Company, FinancialsTransaction, FinancialsTransactionDetails, Supplier, common}
import com.kabasoft.iws.repository.{BankStatementRepository, CompanyRepository, CustomerRepository, SupplierRepository, TransactionRepository}
import zio.prelude.FlipOps
import zio.stream._
import zio.{ZLayer, _}

import java.nio.file.{Files, Paths}
import java.time.Instant
final class BankStatementServiceImpl( bankStmtRepo: BankStatementRepository,
                                      ftrRepo: TransactionRepository,
                                      customerRepo: CustomerRepository,
                                      supplierRepo: SupplierRepository,
                                      companyRepo: CompanyRepository
) extends BankStatementService {

  override def post(id: Long, companyId:String): ZIO[Any, RepositoryError, BankStatement] =
    post(List(id), companyId)*> bankStmtRepo.getById(id)
  override def post(ids: List[Long], companyId: String): ZIO[Any, RepositoryError, List[FinancialsTransaction]] =
    for {
      company <- ZIO.logInfo(s"get company by id  ${companyId}  ") *> companyRepo.getBy(companyId)
      bankStmt <- ZIO.logInfo(s"get bankStmt by ids  ${ids}  ") *> bankStmtRepo.getById(ids).runCollect.map(_.toList)
      ftr <- ZIO.succeed(bankStmt.map(bankStmtRepo.update)) *>
        ZIO.logInfo(s"updated bankStmt  ${bankStmt}  ") *> buildTransactions(bankStmt, company)
      created <- ZIO.logInfo(s"created transactions  ${ftr}  ") *> ftrRepo.create(ftr)
      _ <- ZIO.logInfo(s"Transaction posted ${created}  ")
    } yield created

  private def buildTransactions(bs: List[BankStatement], company: Company): ZIO[Any, RepositoryError, List[FinancialsTransaction]] = bs.map(stmt=>
    (if (stmt.amount.compareTo(zeroAmount) >= 0) {
      customerRepo.getByIban(stmt.accountno, stmt.company)
    } else {
      supplierRepo.getByIban(stmt.accountno, stmt.company)
    }).map(s => buildTransactionFromBankStmt(stmt, s, company))
  ).flip


  private[this] def getAccountOrOaccout(partner: BusinessPartner) =
    if (partner.modelid == Supplier.MODELID) {
      partner.oaccount
    } else {
      partner.account
    }


  private[this] def buildTransactionFromBankStmt(bs: BankStatement, supplier: BusinessPartner, company: Company): FinancialsTransaction = {
    val date   = Instant.now()
    val period = common.getPeriod(Instant.now())

    val l      =
      FinancialsTransactionDetails(
        -1L,
        -1L,
        getAccountOrOaccout(supplier),
        true,
        company.bankAcc,
        bs.amount.abs(),
        bs.valuedate,
        bs.purpose,
        bs.currency
      )
    val tr     = FinancialsTransaction(
      -1L,
      bs.id,
      -1L,
      "100",
      supplier.account,
      bs.valuedate,
      date,
      date,
      period,
      false,
      112,
      bs.company,
      bs.purpose,
      0,
      0,
      List(l)
    )
    tr
  }

  override def importBankStmt(
    path: String,
    header: String,
    char: String,
    extension: String,
    company: String,
    buildFn: String => BankStatement = BankStatement.from
  ): ZIO[Any, RepositoryError, Int] = for {
    bs <- ZStream
            .fromJavaStream(Files.walk(Paths.get(path)))
            .filter(p => !Files.isDirectory(p) && p.toString.endsWith(extension))
            .flatMap { files =>
              ZStream
                .fromPath(files)
                .via(ZPipeline.utfDecode >>> ZPipeline.splitLines)
                .filterNot(p => p.replaceAll(char, "").startsWith(header))
                .map(p => buildFn(p.replaceAll(char, "")))
            } // >>>ZSink.fromZIO(bankStmtRepo.create(_))
            .mapError(e => RepositoryError(e.getMessage))
            .runCollect
            .map(_.toList)
    nr <- bankStmtRepo.create2(bs)
  } yield nr
}

object BankStatementServiceImpl {
  val live: ZLayer[
    BankStatementRepository with TransactionRepository with CustomerRepository with SupplierRepository with CompanyRepository,
    RepositoryError,
    BankStatementService
  ] =
    ZLayer.fromFunction(new BankStatementServiceImpl(_, _, _, _, _))
}
