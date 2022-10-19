package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain._
import com.kabasoft.iws.repository._
import com.kabasoft.iws.service.BankStatementServiceImpl.ZERO
import zio.{ ZLayer, _ }
import zio.stream._

import java.nio.file.{ Files, Paths }
import java.time.Instant

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
      company    <- ZIO.logInfo(s"Query parameters ids ${ids}  with companyId ${companyId}") *>
                     companyRepo.getBy(companyId)
      nr         <- bankStmtRepo.listByIds(ids, companyId)
                      .map(bs => bs.copy(posted = true))
                       .tapSink(sink)
                       .groupByKey(_.amount.compareTo(ZERO) >= 0) { case (cond, stream) =>
                        if (cond) stream.mapZIO(createFTX(_, company))
                        else stream.mapZIO(createFT(_, company))
                      }
                      .mapZIO(ftrRepo.create)
                      .mapError(e => RepositoryError(e))
                      .runCollect
                      .map(_.toList.sum)

    } yield nr

  private[this] def createFT(bs: BankStatement, company: Company): ZIO[Any, RepositoryError, FinancialsTransaction] =
    for {
      supplier <- supplierRepo.getByIban(bs.accountno, bs.company)
    } yield  getFtr4Supplier(bs, supplier, company)

  private[this] def createFTX(bs: BankStatement, company: Company): ZIO[Any, RepositoryError, FinancialsTransaction] =
    for {
      customer <- customerRepo.getByIban(bs.accountno, bs.company)
    } yield getFtr4Customer(bs, customer, company)

  private [this]   def getFtr4Supplier(bs: BankStatement, supplier: Supplier, company: Company): FinancialsTransaction = {
    val date   = Instant.now()
    val period = common.getPeriod(Instant.now())
    val l      =
      FinancialsTransactionDetails(
        -1L,
        -1L,
        supplier.oaccount,
        true,
        company.bankAcc,
        bs.amount,
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
  private [this]  def  getFtr4Customer(
    bs: BankStatement,
    customer: Customer,
    company: Company
  ): FinancialsTransaction = {
    val date   = Instant.now()
    val period = common.getPeriod(Instant.now())
    val l      =
      FinancialsTransactionDetails(
        -1L,
        -1L,
        company.bankAcc,
        true,
        customer.oaccount,
        bs.amount,
        bs.valuedate,
        bs.purpose,
        bs.currency
        // bs.company
      )
    FinancialsTransaction(
      -1L,
      bs.id,
      "100",
      customer.account,
      bs.valuedate,
      date,
      date,
      period,
      false,
      124,
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
