package com.kabasoft.iws.api

import com.kabasoft.iws.domain.Contact
import Html.esc

object PartnerFormView:

  def render(p: Contact): String =
    s"""
       |<form id="partner-form"
       |      class="htmx-grid"
       |      hx-put="/html/partner"
       |      hx-target="#partner-form"
       |      hx-swap="outerHTML">
       |
       |  <input type="hidden" name="id" value="${esc(p.id)}" />
       |  <input type="hidden" name="modelid" value="${p.modelid}" />
       |  <input type="hidden" name="company" value="${esc(p.company)}" />
       |
       |  <label class="daisy-label" for="partner-id">Id</label>
       |  <input id="partner-id" class="daisy-input daisy-input-xs w-full" name="id_display"
       |         value="${esc(p.id)}" readonly />
       |  <label class="daisy-label" for="partner-name">Name</label>
       |  <input id="partner-name" class="daisy-input daisy-input-xs w-full" name="name"
       |         value="${esc(p.name)}" />
       |
       |  <label class="daisy-label" for="partner-street">Street</label>
       |  <input id="partner-street" class="daisy-input daisy-input-xs w-full" name="street"
       |         value="${esc(p.street)}" />
       |  <label class="daisy-label" for="partner-zip">Zip</label>
       |  <input id="partner-zip" class="daisy-input daisy-input-xs w-full" name="zip"
       |         value="${esc(p.zip)}" />
       |
       |  <label class="daisy-label" for="partner-city">City</label>
       |  <input id="partner-city" class="daisy-input daisy-input-xs w-full" name="city"
       |         value="${esc(p.city)}" />
       |  <label class="daisy-label" for="partner-state">State</label>
       |  <input id="partner-state" class="daisy-input daisy-input-xs w-full" name="state"
       |         value="${esc(p.state)}" />
       |
       |  <label class="daisy-label" for="partner-country">Country</label>
       |  <input id="partner-country" class="daisy-input daisy-input-xs w-full" name="country"
       |         value="${esc(p.country)}" />
       |  <label class="daisy-label" for="partner-phone">Phone</label>
       |  <input id="partner-phone" class="daisy-input daisy-input-xs w-full" name="phone"
       |         value="${esc(p.phone)}" />
       |
       |  <label class="daisy-label" for="partner-email">Email</label>
       |  <input id="partner-email" class="daisy-input daisy-input-xs w-full" name="email"
       |         value="${esc(p.email)}" />
       |  <span></span>
       |  <span></span>
       |
       |  <label class="daisy-label" for="partner-desc">Description</label>
       |  <textarea id="partner-desc" class="daisy-textarea daisy-textarea-xs w-full" name="description">${esc(p.description)}</textarea>
       |  <span></span>
       |  <span></span>
       |
       |  <span></span>
       |  <button type="submit" class="daisy-btn btn-primary daisy-btn-xs justify-self-start">Save</button>
       |  <span></span>
       |  <span></span>
       |</form>
       |""".stripMargin