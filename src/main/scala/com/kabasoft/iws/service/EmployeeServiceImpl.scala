package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain._
import com.kabasoft.iws.repository.{AccountRepository, CompanyRepository, EmployeeRepository,
        FinancialsTransactionRepository, PayrollTaxRangeRepository}
import zio._

import java.time.Instant
import java.math.RoundingMode

final class EmployeeServiceImpl (empRepo: EmployeeRepository,  accountRepo: AccountRepository, companyRepo: CompanyRepository,
                                 ptrRepo: PayrollTaxRangeRepository, ftrRepo: FinancialsTransactionRepository) extends EmployeeService {

  override def generate(company: String): ZIO[Any, RepositoryError, Int] = for {
    _<- ZIO.logDebug(s" Posting transaction for the company ${company}")
    transactions <- build(company).debug("transactions")
    nr<-  ZIO.succeed(transactions).map(_.size) //ftrRepo.create(transactions).map(_.size)
  }yield nr

  private def build(companyId: String): ZIO[Any, RepositoryError, List[FinancialsTransaction]] = for {
    company <- companyRepo.getBy( companyId).debug("company")
    employee <- empRepo.all((Employee.MODELID, companyId)).debug("employee")
    accounts<- accountRepo.all((Account.MODELID, companyId))//.debug("accounts")
    ptr <- ptrRepo.all((PayrollTaxRange.MODELID, companyId))//.debug("payroll gross salary to tax map ")
  }yield employee.map(emp => buildTransaction(emp,
          buildTransactionDetails (emp, emp.salaryItems.map(EmployeeSalaryItem.apply), accounts, company, ptr) ))

  private def buildTransactionDetails(emp:Employee, salaryItems: List[EmployeeSalaryItem], accounts:List[Account],
                                             company: Company, ptr:List[PayrollTaxRange]) = {
    salaryItems.map(item => FinancialsTransactionDetails(-1L, -1L, emp.oaccount, side = true, item.account
      , emp.salary.multiply(item.percentage).setScale(6, RoundingMode.HALF_UP)
      , Instant.now(), item.text, company.currency, getName(accounts, emp.oaccount), getName(accounts, item.account)))
  }
  def getName (accounts:List[Account], id:String): String =
    accounts.find(_.id == id).fold(s"Account with id ${id} not found!!!")(_.name)

  private def buildTransaction(emp:Employee,  lines: List[FinancialsTransactionDetails]) = {
    val date = Instant.now()
    val period = common.getPeriod(date)
    FinancialsTransaction(
      -1L,
      -1L,
      -1L,
      "100",
      emp.account,
      date,
      date,
      date,
      period,
      posted = false,
      TransactionModelId.PAYROLL.id,
      emp.company,
      "Salary "+period,
      0,
      0,
      lines
    )
  }
}
object EmployeeServiceImpl {
  val live: ZLayer[EmployeeRepository  with AccountRepository with FinancialsTransactionRepository with CompanyRepository
                                       with PayrollTaxRangeRepository,  RepositoryError, EmployeeService] =
    ZLayer.fromFunction(new EmployeeServiceImpl(_, _, _, _, _))
}

