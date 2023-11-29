package com.kabasoft.iws.api

import com.kabasoft.iws.api.AccountEndpoint.{accByIdEndpoint, accCreateEndpoint, accDeleteEndpoint}
import com.kabasoft.iws.api.BankEndpoint.{bankByIdEndpoint, bankCreateEndpoint, bankDeleteEndpoint}
import zio.json.EncoderOps
import com.kabasoft.iws.repository.{AccountCache, AccountCacheImpl, AccountRepositoryImpl, BankCache, BankCacheImpl,
  BankRepositoryImpl, CostcenterCache, CostcenterCacheImpl, CostcenterRepositoryImpl, CustomerCache, CustomerCacheImpl,
  CustomerRepositoryImpl, FinancialsTransactionCache, FinancialsTransactionCacheImpl, ModuleCache, ModuleCacheImpl,
  ModuleRepositoryImpl, SupplierCache, SupplierCacheImpl, SupplierRepositoryImpl, FinancialsTransactionRepository,
  FinancialsTransactionRepositoryImpl, UserRepository, UserRepositoryImpl, VatCache, VatCacheImpl, VatRepositoryImpl}
import com.kabasoft.iws.api.CostcenterEndpoint.{ccByIdEndpoint, ccCreateEndpoint, ccDeleteEndpoint}
import com.kabasoft.iws.api.Protocol._
import com.kabasoft.iws.domain.BankBuilder.{bank, bankx}
import com.kabasoft.iws.api.CustomerEndpoint.{custByIdEndpoint, custCreateEndpoint, custDeleteEndpoint}
import com.kabasoft.iws.api.FinancialsEndpoint.ftrModifyEndpoint
import com.kabasoft.iws.api.ModuleEndpoint.{moduleByIdEndpoint, moduleCreateEndpoint, moduleDeleteEndpoint}
import com.kabasoft.iws.api.SupplierEndpoint.{supByIdEndpoint, supCreateEndpoint, supDeleteEndpoint}
import com.kabasoft.iws.api.UserEndpoint.{userByUserNameEndpoint, userDeleteEndpoint}
import com.kabasoft.iws.api.VatEndpoint.{vatByIdEndpoint, vatCreateEndpoint, vatDeleteEndpoint}
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
import com.kabasoft.iws.repository.Schema._
import zio.http.endpoint.EndpointMiddleware.None
import zio.http.Status
import zio.http.{Request, URL}
import zio.schema.DeriveSchema.gen
import zio.sql.ConnectionPool
import zio.test._

object ApiSpec extends ZIOSpecDefault {

    def spec = suite("APISpec")(
      suite("handler")(
        test("Account  integration test ") {
        val accAll = Endpoint.get(("acc")/string("company")).out[Int].outError[RepositoryError](Status.InternalServerError)
                     .implement(company => AccountCache.all(company).mapBoth(e => RepositoryError(e.getMessage), _.size))
          val testRoutes = testApi((accAll ++accByIdEndpoint)) _
          val deleteRoutes = testDeleteApi(accDeleteEndpoint) _
          val postRoutes = testPostApi(accCreateEndpoint) _
            testRoutes("/acc/1000", "17") && testRoutes("/acc/"+acc.id+"/"+acc.company, acc.toJson)&&
            deleteRoutes("/acc/"+acc.id+"/"+acc.company, "1")&&
            postRoutes("/acc", accx.toJson, accx.toJson)
        },

        test("financials integration test") {
          val ftrByModelId = Endpoint.get("ftr1" / string("company") / int("modelid")).out[Int].outError[RepositoryError](Status.InternalServerError)
            .implement(p => ZIO.logInfo(s"Find  Transaction by modelId  ${p}") *>
              FinancialsTransactionCache.getByModelId((p._2, p._1)).mapBoth(e => RepositoryError(e.getMessage), _.size))
          val ftrByTransId = Endpoint.get("ftr2" / string("company") / int("transid")).out[BigDecimal].outError[RepositoryError](Status.InternalServerError)
            .implement(p => ZIO.logInfo(s"Find  Transaction by TransId  ${p}") *>
              FinancialsTransactionRepository.getByTransId((p._2.toLong, p._1)).mapBoth(e => RepositoryError(e.getMessage), _.total))

         // val testRoutes1 = testPostApi(ftrCreateEndpoint) _
          val testRoutes = testApi(ftrByModelId ++ ftrByTransId ++ ftrModifyEndpoint) _
          //val testRoutes2 = testPutApi( ftrModifyEndpoint) _
         // val payload = ftr5.toJson
         // val result = ftr5.toJson//.toString.replaceAll("terms","modified")

          //val deleteRoutes = testDeleteApi(bankDeleteEndpoint) _

          //testRoutes1("/ftr", FTR, "2") &&
          testRoutes("/ftr2/1000/1" , "100.00") && testRoutes("/ftr1/1000/"+124, "1") //&&
          //testRoutes1("/ftr", ftr4.toJson, ftr4.toJson.replaceAll("id:-1", "id:2"))
          //testRoutes2("/ftr", payload, payload)
          // deleteRoutes("/bank/" + bank.id + "/" + bank.company, "1") && testRoutes1("/bank", bankx.to)Json, "1")
        },
        test("Bank integration test") {
          val bankAll = Endpoint.get("bank"/string("company")).out[Int].outError[RepositoryError](Status.InternalServerError)
            .implement ( company => BankCache.all(company).mapBoth(e => RepositoryError(e.getMessage), _.size))
          val testRoutes = testApi(bankAll ++ bankByIdEndpoint++bankCreateEndpoint) _
          val deleteRoutes = testDeleteApi(bankDeleteEndpoint) _
          val testRoutes1 = testPostApi(bankCreateEndpoint) _
          testRoutes("/bank/"+bank.company, "2") && testRoutes("/bank/"+bank.id+"/"+bank.company, bank.toJson) &&
            deleteRoutes("/bank/"+bank.id+"/"+bank.company, "1")&& testRoutes1("/bank", bankx.toJson, bankx.toJson)
        },
         test("Customer integration test") {
          val custAllEndpoint = Endpoint.get("cust"/string("company")).out[Int].outError[RepositoryError](Status.InternalServerError)
            .implement(company => CustomerCache.all(company).mapBoth(e => RepositoryError(e.getMessage), _.size))
          val testRoutes = testApi(custByIdEndpoint ++ custAllEndpoint) _
           val deleteRoutes = testDeleteApi(custDeleteEndpoint) _
          val testRoutes1 = testPostApi(custCreateEndpoint) _
          testRoutes("/cust/"+cust.company, "3") && testRoutes("/cust/"+cust.id+"/"+cust.company, cust.toJson)&&
          deleteRoutes("/cust/"+cust.id+"/"+cust.company, "1")&&testRoutes1("/cust", custx.toJson, custx.toJson)
        },
        test("Supplier integration test") {
          val supAllEndpoint = Endpoint.get("sup"/string("company")).out[Int].outError[RepositoryError](Status.InternalServerError)
            .implement(company => SupplierCache.all(company).mapBoth(e => RepositoryError(e.getMessage), _.size))
          val testRoutes = testApi(supByIdEndpoint ++ supAllEndpoint) _
          val deleteRoutes = testDeleteApi(supDeleteEndpoint) _
          val testRoutes1 = testPostApi(supCreateEndpoint) _
          testRoutes("/sup/"+sup.company, "6") && testRoutes("/sup/"+sup.id+"/"+sup.company, sup.toJson)&&
          deleteRoutes("/sup/"+sup.id+"/"+sup.company, "1")&&testRoutes1("/sup", supx.toJson, supx.toJson)
        },
        test("Cost center integration test") {
          val ccAllEndpoint = Endpoint.get("cc"/string("company")).out[Int].outError[RepositoryError](Status.InternalServerError)
            .implement((company:String) => CostcenterCache.all(company).mapBoth(e => RepositoryError(e.getMessage), _.size))
          val testRoutes = testApi(ccByIdEndpoint ++ ccAllEndpoint) _
          val deleteRoutes = testDeleteApi(ccDeleteEndpoint) _
          val testRoutes1 = testPostApi(ccCreateEndpoint) _
          testRoutes("/cc/"+cc.company, "2") && testRoutes("/cc/"+cc.id+"/"+cc.company, cc.toJson)&&
            deleteRoutes("/cc/"+cc.id+"/"+cc.company, "1")&&testRoutes1("/cc", ccx.toJson, ccx.toJson)
        },
          test("Module integration test") {
          val moduleAllEndpoint = Endpoint.get("module"/string("company")).out[Int].outError[RepositoryError](Status.InternalServerError)
            .implement((company:String) => ModuleCache.all(company).mapBoth(e => RepositoryError(e.getMessage), _.size))
          val testRoutes = testApi(moduleByIdEndpoint ++ moduleAllEndpoint) _
            val deleteRoutes = testDeleteApi(moduleDeleteEndpoint) _
          val testRoutes1 = testPostApi(moduleCreateEndpoint) _
          testRoutes("/module/"+m.company, "1") && testRoutes("/module/"+m.id+"/"+m.company, m.toJson)&&
            deleteRoutes("/module/"+m.id+"/"+m.company, "1")&&testRoutes1("/module", mx.toJson, mx.toJson)
        },
        test("Vat integration test") {
          val vatAllEndpoint = Endpoint.get("vat"/string("company")).out[Int].outError[RepositoryError](Status.InternalServerError)
            .implement((company:String) => VatCache.all(company).mapBoth(e => RepositoryError(e.getMessage), _.size))
          val testRoutes = testApi(vatByIdEndpoint ++ vatAllEndpoint) _
          val deleteRoutes = testDeleteApi(vatDeleteEndpoint) _
          val testRoutes1 = testPostApi(vatCreateEndpoint) _
          testRoutes("/vat/1000", "2") && testRoutes("/vat/"+vat1.id+"/"+vat1.company, vat1.toJson) &&
          deleteRoutes("/vat/"+vat1.id+"/"+vat1.company, "1")&&testRoutes1("/vat", vat1x.toJson, vat1x.toJson)
        },
        test("User integration test") {
          val userAllEndpoint = Endpoint.get("user" / string("company")).out[Int].outError[RepositoryError](Status.InternalServerError)
            .implement((company: String) => UserRepository.all(company).mapBoth(e => RepositoryError(e.getMessage), _.size))
          val testRoutes = testApi(userByUserNameEndpoint ++ userAllEndpoint) _
          val deleteRoutes = testDeleteApi(userDeleteEndpoint) _
          //val testRoutes1 = testPostApi(userCreateEndpoint) _
          testRoutes("/user/1000", "2") && testRoutes("/user/" + user.userName + "/" + user.company, user.toJson) &&
            deleteRoutes("/user/" + userx.id + "/" + userx.company, "1") //&& testRoutes1("/user", userx.toJson, "1")
        }
    ).provideShared(ConnectionPool.live,  AccountRepositoryImpl.live, AccountCacheImpl.live,BankRepositoryImpl.live,
        BankCacheImpl.live, CostcenterRepositoryImpl.live, CostcenterCacheImpl.live, CustomerRepositoryImpl.live,
        CustomerCacheImpl.live, SupplierRepositoryImpl.live, SupplierCacheImpl.live, VatRepositoryImpl.live, VatCacheImpl.live,
        ModuleRepositoryImpl.live, ModuleCacheImpl.live, UserRepositoryImpl.live, PostgresContainer.connectionPoolConfigLayer,
        FinancialsTransactionRepositoryImpl.live, FinancialsTransactionCacheImpl.live,
        PostgresContainer.createContainer)
    )

  def testApi[R, E](service: Routes[R, E, None])(
    url: String, expected: String): ZIO[R, Response, TestResult] = {
    val request = Request.get(url = URL.decode(url).toOption.get)
    for {
      response <- service.toApp.runZIO(request).mapError(_.get)
      body <- response.body.asString.orDie
    } yield assertTrue(body == expected)
  }

  def testDeleteApi[R, E](service: Routes[R, E, None])(
    url: String, expected: String): ZIO[R, Response, TestResult] = {
    val request = Request.delete(url = URL.decode(url).toOption.get)
    for {
      response <- service.toApp.runZIO(request).mapError(_.get)
      body <- response.body.asString.orDie
    } yield assertTrue(body == expected)
  }

  def testPostApi[R, E](routes: Routes[R, E, None])(
    url: String, body: String, expected: String): ZIO[R, Response, TestResult] = {
    val request = Request.post(Body.fromString(body), URL.decode(url).toOption.get)
    for {
      response <- routes.toApp.runZIO(request).mapError(_.get)
      body <- response.body.asString.orDie
    } yield assertTrue(body == expected)
  }
  def testPutApi[R, E](routes:  Routes[R, E, None] )(
    url: String, body:String, expected: String): ZIO[R, Response, TestResult] = {
    val request = Request.put(Body.fromString(body), URL.decode(url).toOption.get)
    for {
      response <- routes.toApp.runZIO(request).mapError(_.get)
      body <- response.body.asString.orDie
    } yield assertTrue(body == expected)
  }
}

