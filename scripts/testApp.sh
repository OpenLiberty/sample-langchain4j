#!/bin/bash
set -euxo pipefail
./mvnw -version

./mvnw -Dhttp.keepAlive=false \
    -Dmaven.wagon.http.pool=false \
    -Dmaven.wagon.httpconnectionManager.ttlSeconds=120 \
    -q clean package liberty:create liberty:install-feature liberty:deploy

./mvnw liberty:start
./mvnw failsafe:integration-test liberty:stop
./mvnw failsafe:verify