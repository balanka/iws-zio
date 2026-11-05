#!/usr/bin/env bash
set -u
CURL=/usr/bin/curl   # force absolute path
BASE="https://kabasoft.iws"
paths=(
  "api/users/login"
  "html/financials/new-form?modelid=1300&company=5700"
  "html/transaction/new-form?modelid=1301&company=5700"
  "financials-combined-htmx/1000"
  "transaction-combined-htmx/1000"
  "html/financials-combined/1000"
  "html/transaction-combined/1000"
)
for p in "${paths[@]}"; do
  code=$("$CURL" -sk -o /dev/null -w '%{http_code}' "$BASE/$p")
  printf '%-55s %s\n' "$p" "$code"
done
