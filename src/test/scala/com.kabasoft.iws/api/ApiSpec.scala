package com.kabasoft.iws.api

import com.kabasoft.iws.api.AccountEndpoint.{accByIdEndpoint, accCreteEndpoint}
import com.kabasoft.iws.api.BankEndpoint.{bankByIdEndpoint, bankCreateEndpoint}
import zio.json.EncoderOps
import com.kabasoft.iws.repository.{AccountRepository, AccountRepositoryImpl, BankRepository, BankRepositoryImpl, CostcenterRepository, CostcenterRepositoryImpl, CustomerRepository, CustomerRepositoryImpl, ModuleRepository, ModuleRepositoryImpl, SupplierRepository, SupplierRepositoryImpl, UserRepository, UserRepositoryImpl, VatRepository, VatRepositoryImpl}
import com.kabasoft.iws.api.CostcenterEndpoint.{ccByIdEndpoint, ccCreateEndpoint}
import com.kabasoft.iws.api.Protocol.{vatEncoder, _}
import com.kabasoft.iws.domain.BankBuilder.{bank, bankx}
import com.kabasoft.iws.api.CustomerEndpoint.{custByIdEndpoint, custCreateEndpoint}
import com.kabasoft.iws.api.ModuleEndpoint.{moduleByIdEndpoint, moduleCreateEndpoint}
import com.kabasoft.iws.api.SupplierEndpoint.{supByIdEndpoint, supCreateEndpoint}
import com.kabasoft.iws.api.UserEndpoint.{userByUserNameEndpoint, userCreateEndpoint}
import com.kabasoft.iws.api.VatEndpoint.{vatByIdEndpoint, vatCreateEndpoint}
import com.kabasoft.iws.domain.AccountBuilder.{acc, accx}
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.CostcenterBuilder.ccx
import com.kabasoft.iws.domain.CustomerBuilder.{cust, custx}
import com.kabasoft.iws.domain.ModuleBuilder.mx
import com.kabasoft.iws.domain.SupplierBuilder.supx
import com.kabasoft.iws.domain.UserBuilder.userx
import com.kabasoft.iws.domain.VatBuilder.vat1x
import com.kabasoft.iws.repository.container.PostgresContainer
import zio.http.{Body, Response}
import com.kabasoft.iws.domain.CostcenterBuilder.cc
import com.kabasoft.iws.domain.ModuleBuilder.m
import com.kabasoft.iws.domain.SupplierBuilder.sup
import com.kabasoft.iws.domain.UserBuilder.user
import com.kabasoft.iws.domain.VatBuilder.vat1
import zio._
import zio.http.endpoint.{Endpoint, Routes}
import zio.http.codec.HttpCodec._
import zio.http.endpoint.EndpointMiddleware.None
import zio.http.model.Status
import zio.http.{Request, URL}
import zio.schema.DeriveSchema.gen
import zio.sql.ConnectionPool
import zio.test._

object ApiSpec extends ZIOSpecDefault {

    def spec = suite("APISpec")(
      suite("handler")(
        test("Account  integration test ") {
        val accAll = Endpoint.get("acc").out[Int].outError[RepositoryError](Status.InternalServerError)
                     .implement(_ => AccountRepository.all("1000").mapBoth(e => RepositoryError(e.getMessage), _.size))
          val testRoutes = testApi((accByIdEndpoint ++ accAll)) _
          val testRoutes1 = testPostApi(accCreteEndpoint) _
          testRoutes("/acc/1000", "14") && testRoutes("/acc/"+acc.id, acc.toJson)&& testRoutes1("/acc", List(accx).toJson, "")
        },
        test("Bank integration test") {
          val bankAll = Endpoint.get("bank"/string("company")).out[Int].outError[RepositoryError](Status.InternalServerError)
            .implement ( company => BankRepository.all(company).mapBoth(e => RepositoryError(e.getMessage), _.size))
          val testRoutes = testApi(bankAll ++ bankByIdEndpoint) _
          val testRoutes1 = testPostApi(bankCreateEndpoint) _
          testRoutes("/bank/1000", "2") && testRoutes("/bank/"+bank.id, bank.toJson) && testRoutes1("/bank", List(bankx).toJson, "")
        },
         test("Customer integration test") {
          val custAllEndpoint = Endpoint.get("cust"/string("company")).out[Int].outError[RepositoryError](Status.InternalServerError)
            .implement((company:String) => CustomerRepository.all(company).mapBoth(e => RepositoryError(e.getMessage), _.size))
          val testRoutes = testApi(custByIdEndpoint ++ custAllEndpoint) _
          val testRoutes1 = testPostApi(custCreateEndpoint) _
          testRoutes("/cust/1000", "3") && testRoutes("/cust/"+cust.id, cust.toJson)&& testRoutes1("/cust", List(custx).toJson, "")
        },
        test("Supplier integration test") {
          val supAllEndpoint = Endpoint.get("sup"/string("company")).out[Int].outError[RepositoryError](Status.InternalServerError)
            .implement((company:String) => SupplierRepository.all(company).mapBoth(e => RepositoryError(e.getMessage), _.size))
          val testRoutes = testApi(supByIdEndpoint ++ supAllEndpoint) _
          val testRoutes1 = testPostApi(supCreateEndpoint) _
          testRoutes("/sup/1000", "6") && testRoutes("/sup/"+sup.id, sup.toJson)&& testRoutes1("/sup", List(supx).toJson, "")
        },
        test("Cost center integration test") {
          val ccAllEndpoint = Endpoint.get("cc"/string("company")).out[Int].outError[RepositoryError](Status.InternalServerError)
            .implement((company:String) => CostcenterRepository.all(company).mapBoth(e => RepositoryError(e.getMessage), _.size))
          val testRoutes = testApi(ccByIdEndpoint ++ ccAllEndpoint) _
          val testRoutes1 = testPostApi(ccCreateEndpoint) _
          testRoutes("/cc/1000", "2") && testRoutes("/cc/"+cc.id, cc.toJson)&& testRoutes1("/cc", List(ccx).toJson, "")
        },
          test("Module integration test") {
          val moduleAllEndpoint = Endpoint.get("module"/string("company")).out[Int].outError[RepositoryError](Status.InternalServerError)
            .implement((company:String) => ModuleRepository.all(company).mapBoth(e => RepositoryError(e.getMessage), _.size))
          val testRoutes = testApi(moduleByIdEndpoint ++ moduleAllEndpoint) _
          val testRoutes1 = testPostApi(moduleCreateEndpoint) _
          testRoutes("/module/1000", "1") && testRoutes("/module/"+m.id, m.toJson)&& testRoutes1("/module", List(mx).toJson, "")
        },
           test("User integration test") {
          val userAllEndpoint = Endpoint.get("user"/string("company")).out[Int].outError[RepositoryError](Status.InternalServerError)
            .implement((company:String) => UserRepository.all(company).mapBoth(e => RepositoryError(e.getMessage), _.size))
          val testRoutes = testApi(userByUserNameEndpoint ++ userAllEndpoint) _
         val testRoutes1 = testPostApi(userCreateEndpoint) _
          testRoutes("/user/1000", "2") && testRoutes("/user/"+user.userName, user.toJson)&& testRoutes1("/user", List(userx).toJson, "")
        },
        test("Vat integration test") {
          val vatAllEndpoint = Endpoint.get("vat"/string("company")).out[Int].outError[RepositoryError](Status.InternalServerError)
            .implement((company:String) => VatRepository.all(company).mapBoth(e => RepositoryError(e.getMessage), _.size))
          val testRoutes = testApi(vatByIdEndpoint ++ vatAllEndpoint) _
          val testRoutes1 = testPostApi(vatCreateEndpoint) _
          testRoutes("/vat/1000", "2") && testRoutes("/vat/"+vat1.id, vat1.toJson) && testRoutes1("/vat", List(vat1x).toJson, "")

        }
    ).provide(ConnectionPool.live,  AccountRepositoryImpl.live, BankRepositoryImpl.live,
        CostcenterRepositoryImpl.live, CustomerRepositoryImpl.live, SupplierRepositoryImpl.live, VatRepositoryImpl.live,
        ModuleRepositoryImpl.live, UserRepositoryImpl.live, PostgresContainer.connectionPoolConfigLayer, PostgresContainer.createContainer)
    )

  def testApi[R, E](service: Routes[R, E, None])(
    url: String, expected: String): ZIO[R, Response, TestResult] = {
    val request = Request.get(url = URL.fromString(url).toOption.get)
    for {
      response <- service.toApp.runZIO(request).mapError(_.get)
      body <- response.body.asString.orDie
    } yield assertTrue(body == expected)
  }

  def testPostApi[R, E](routes:  Routes[R, E, None] )(
    url: String, body:String, expected: String): ZIO[R, Response, TestResult] = {
    val request = Request.post(Body.fromString(body), URL.fromString(url).toOption.get)
    for {
      response <- routes.toApp.runZIO(request).mapError(_.get)
      body <- response.body.asString.orDie
    } yield assertTrue(body == expected)
  }
}

