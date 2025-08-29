package com.kabasoft.iws.domain

import com.kabasoft.iws.domain.AccountBuilder.{expenseaccountId, stockaccountId, revenueaccountId}

import java.math.BigDecimal
import java.time.Instant

object ArticleBuilder:
  val companyId ="-1000"
  val artId0 = "0000"
  val artName0 = "Atikel_0"
  val artId1 = "0001"
  val artName1 = "Atikel_1"
  val quantityUnit = "stk"
  val vatCode = "v5"
  val ccy = "EUR"
  val t: Instant = Instant.now()
  val zero: BigDecimal = BigDecimal.valueOf(0, 2 )
  
  val ART0 =  Article(artId0, artName0, artName0, "-1", zero, zero, zero, ccy, stocked =true, quantityUnit, quantityUnit, stockaccountId, expenseaccountId, revenueaccountId, vatCode, companyId, Article.MODELID, t , t, t, Nil)
  val ART1 =  Article(artId1, artName1, artName1, "-1", zero, zero, zero, ccy, stocked =true, quantityUnit, quantityUnit, stockaccountId, expenseaccountId, revenueaccountId, vatCode, companyId, Article.MODELID, t , t, t, Nil)

  val articleList =List(ART0, ART1)