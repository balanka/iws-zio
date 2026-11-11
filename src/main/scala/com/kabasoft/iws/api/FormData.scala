package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{Account, Article, Contact, Fmodule, Store, Vat}

/** The set of masterfile lists a form view usually needs.
 * All fields default to Nil so partial construction is easy.
 */

final case class BaseFormData(
                               accounts: List[Account],
                               stores:   List[Store],
                               contacts: List[Contact],
                               fmodules: List[Fmodule])

final case class TransactionFormData(
                                      base:     BaseFormData,
                                      articles: List[Article],
                                      vats:     List[Vat])
final case class FormData(
                           accounts: List[com.kabasoft.iws.domain.Account],
                           stores:   List[com.kabasoft.iws.domain.Store],
                           contacts: List[com.kabasoft.iws.domain.Contact],
                           fmodules: List[Fmodule],
                           articles: List[com.kabasoft.iws.domain.Article],
                           vats:     List[com.kabasoft.iws.domain.Vat]
                         )
