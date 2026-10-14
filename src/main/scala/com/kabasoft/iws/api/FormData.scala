package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{Account, Article, Contact, Fmodule, Store, Vat}

/** The set of masterfile lists a form view usually needs.
 * All fields default to Nil so partial construction is easy.
 */
case class FormData(accounts: List[Account] = Nil,
                     articles: List[Article] = Nil,
                     vats:     List[Vat]     = Nil,
                     stores:   List[Store]   = Nil,
                     contacts: List[Contact] = Nil,
                     fmodules: List[Fmodule] = Nil
                   )