#!/bin/bash
set -a
source ./Users/iwsmac/Downloads/tools/scala/samples/zio/iws-zio/IWS_DEV.env          # or IWS_DEV.env — whichever holds your DB creds
set +a

export IWS_API_HOST=0.0.0.0
export IWS_API_PORT=8080
export POSTGRES_HOST=localhost
export POSTGRES_PORT=5432

./target/native-image/iws-api
