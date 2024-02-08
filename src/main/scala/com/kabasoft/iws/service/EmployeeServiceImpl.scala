package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain._
import com.kabasoft.iws.repository.{AccountRepository, EmployeeRepository, FinancialsTransactionRepository}
import zio._
import java.time.Instant
import java.math.RoundingMode

final class EmployeeServiceImpl (empRepo: EmployeeRepository,  accountRepo: AccountRepository, ftrRepo: FinancialsTransactionRepository) extends EmployeeService {

  override def generate(modelid:Int, company: String): ZIO[Any, RepositoryError, Int] = for {
    //<- ZIO.logInfo(s" Posting transaction with id ${id} of company ${company}")
    transactions <- build(modelid, company)
    nr<-  ftrRepo.create(transactions).map(_.size)
  }yield nr

  private def build(modelid:Int, company: String): ZIO[Any, RepositoryError, List[FinancialsTransaction]] = for {
    //<- ZIO.logInfo(s" Posting transaction with id ${id} of company ${company}")
    employee <- empRepo.all((Employee.MODELID, company))
    accounts<- accountRepo.all((Account.MODELID, company))
  }yield employee.map(emp => buildTransaction(emp,  modelid, buildTransactionDetails (emp, emp.salaryItems, accounts) ))

  private def buildTransactionDetails(emp:Employee, salaryItems: List[EmployeeSalaryItem], accounts:List[Account]) = {
    salaryItems.map(item => FinancialsTransactionDetails(-1L, -1L, emp.account, true, item.account
      , emp.salary.multiply(item.percentage).setScale(6, RoundingMode.HALF_UP)
      , Instant.now(), item.text, "EUR", getName(accounts, emp.account), getName(accounts, item.account)))
  }
  def getName (accounts:List[Account], id:String): String =
    accounts.find(_.id == id).fold(s"Account with id ${id} not found!!!")(_.name)

  private def buildTransaction(emp:Employee,  modelid:Int,  lines: List[FinancialsTransactionDetails]) = {
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
      false,
      modelid,
      emp.company,
      "Salary "+period,
      0,
      0,
      lines
    )
  }
}
object EmployeeServiceImpl {
  val live: ZLayer[EmployeeRepository  with AccountRepository with FinancialsTransactionRepository,  RepositoryError, EmployeeService] =
    ZLayer.fromFunction(new EmployeeServiceImpl(_, _, _))
}

