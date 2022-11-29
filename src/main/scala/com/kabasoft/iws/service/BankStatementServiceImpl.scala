package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain._
import com.kabasoft.iws.repository._
import com.kabasoft.iws.service.BankStatementServiceImpl.ZERO
import zio.prelude.AssociativeComposeOps
import zio.{ZLayer, _}
import zio.stream._

import java.nio.file.{Files, Paths}
import java.time.Instant
import scala.math.Numeric.BigDecimalAsIfIntegral.abs

final class BankStatementServiceImpl(
  bankStmtRepo: BankStatementRepository,
  ftrRepo: TransactionRepository,
  customerRepo: CustomerRepository,
  supplierRepo: SupplierRepository,
  companyRepo: CompanyRepository
) extends BankStatementService {

  private[this] def  sink: ZSink[Any, RepositoryError, BankStatement, RepositoryError, List[zio.ZIO[Any, RepositoryError, Int]]] =
     ZSink.collectAll[BankStatement].map(_.toList.map(bankStmtRepo.modify(_)))

  override def postAll(ids: List[Long], companyId: String) : ZIO[Any, RepositoryError, Int] =

    for {
      company    <- ZIO.logDebug(s"Query parameters ids ${ids}  with companyId ${companyId}") *>
                     companyRepo.getBy(companyId)
      nrTransaction         <- bankStmtRepo.listByIds(ids, companyId)
                      .map(bs => bs.copy(posted = true))
                       .tapSink(sink)
                       .mapZIO(buildTransactions(_, company)
                         .flatMap(ftrRepo.create))
                      .mapError(e => RepositoryError(e))
                      .runCollect
                      .map(_.toList.sum)

    } yield nrTransaction

  private[this] def buildTransactions(bs: BankStatement, company: Company): ZIO[Any, RepositoryError, FinancialsTransaction] =
    if(bs.amount >= 0){
      customerRepo.getByIban(bs.accountno, bs.company).map(buildTransactionFromBankStmt(bs, _, company))
    } else{
      supplierRepo.getByIban(bs.accountno, bs.company).map(buildTransactionFromBankStmt(bs, _, company))
    }

  private [this]   def getAccountOrOaccout(supplier: BusinessPartner) =
    if(supplier.modelid==Supplier.MODELID ) {
      supplier.oaccount
    }else {
      supplier.account
    }

  private [this]   def buildTransactionFromBankStmt(bs: BankStatement, supplier: BusinessPartner, company: Company): FinancialsTransaction = {
    val date   = Instant.now()
    val period = common.getPeriod(Instant.now())
    val l      =
      FinancialsTransactionDetails(
        -1L,
        -1L,
        getAccountOrOaccout(supplier),
        true,
        company.bankAcc,
        abs(bs.amount),
        bs.valuedate,
        bs.purpose,
        bs.currency
        // bs.company
      )
    FinancialsTransaction(
      -1L,
      bs.id,
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
            .mapError(e => RepositoryError(e))
            .runCollect
            .map(_.toList)
    nr <- bankStmtRepo.create(bs)
  } yield nr
}

object BankStatementServiceImpl {
  val ZERO = BigDecimal(0)
  val live: ZLayer[
    BankStatementRepository with TransactionRepository with CustomerRepository with SupplierRepository with CompanyRepository,
    RepositoryError,
    BankStatementService
  ] =
    ZLayer.fromFunction(new BankStatementServiceImpl(_, _, _, _, _))
}
