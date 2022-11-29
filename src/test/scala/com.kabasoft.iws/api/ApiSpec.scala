package com.kabasoft.iws.api

import com.kabasoft.iws.api.Protocol._
import com.kabasoft.iws.config.DbConfig
import com.kabasoft.iws.config.DbConfig.connectionPoolConfig
import com.kabasoft.iws.domain.{Account, Bank}
import com.kabasoft.iws.repository.{AccountRepository, AccountRepositoryImpl, BankRepository, BankRepositoryImpl}
import zio._
import zio.http.api.RouteCodec._
import zio.http.api.{EndpointSpec, Endpoints}
import zio.http.{Request, URL}
import zio.json._
import zio.sql.ConnectionPool
import zio.test._

import java.nio.charset.Charset
import java.time.Instant
import scala.language.postfixOps

object ApiSpec extends ZIOSpecDefault {
  val company = "1000"
  val bankId = "COLSDE33"
  val bankName = "SPARKASSE KOELN-BONN"
  val accId = "9900"
  val accName = "Bilanz"
  val bank = Bank(bankId, bankName, bankName, instantFromStr("2018-01-02T01:00:00.00Z"),
    instantFromStr("2018-01-02T01:00:00.00Z"), instantFromStr("2018-01-02T01:00:00.00Z"),11,company)
  val acc = Account(accId, accName, accName, instantFromStr("2017-12-31T23:00:00Z"), instantFromStr("2017-12-31T23:00:00Z")
    , instantFromStr("2020-03-01T23:00:00Z"), company, 9, "", true, true, "EUR", BigDecimal(0.00), BigDecimal(0.00)
    , BigDecimal(0.00), BigDecimal(0.00), Nil.toSet)


  def instantFromStr(str:String)=Instant.parse(str)
    def spec = suite("APISpec")(
      suite("handler")(
        test("simple request") {
          val testRoutes = testApi(
            EndpointSpec
              .get(literal("acc") / string)
              .out[Account]
              .implement { id => AccountRepository.getBy(id, "1000")
              } ++
              EndpointSpec
                .get(literal("acc"))
                .out[Int]
                .implement { case () => AccountRepository.all("1000").map(_.size)
                }++
            EndpointSpec
              .get(literal("bank") / string)
              .out[Bank]
              .implement { bankId =>BankRepository.getBy(bankId,"1000")
              } ++
              EndpointSpec
                .get(literal("bank"))
                .out[Int]
                .implement { case () => BankRepository.all("1000").map(_.size)
                }
          )  _
         // testRoutes("/bank/"+bankId, bank.toJson) &&
          testRoutes("/bank", 56+"") &&
           testRoutes("/acc", 325+"")
          //testRoutes("/acc/"+accId, acc.toJson)&& testRoutes("/acc", 321+"")
        }.provide( ConnectionPool.live, connectionPoolConfig, DbConfig.layer,
          BankRepositoryImpl.live, AccountRepositoryImpl.live),
      )
    )
  def testApi[R, E](service: Endpoints[R, E, _])(
    url: String, expected: String): ZIO[R, E, TestResult] = {
    val request = Request.get(url = URL.fromString(url).toOption.get)
    for {
      response <- service.toHttpApp(request).mapError(_.get)
      body <- response.body.asString.orDie//(Charset.forName("UTF-8")).orDie
    } yield assertTrue(body == expected)
  }
}