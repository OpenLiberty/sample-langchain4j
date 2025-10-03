#!/bin/bash
set -euxo pipefail
./mvnw -version

./mvnw -ntp -Dhttp.keepAlive=false \
    -Dmaven.wagon.http.pool=false \
    -Dmaven.wagon.httpconnectionManager.ttlSeconds=120 \
    -q -pl server clean package liberty:create liberty:install-feature liberty:deploy

./mvnw -ntp -Dhttp.keepAlive=false \
    -Dmaven.wagon.http.pool=false \
    -Dmaven.wagon.httpconnectionManager.ttlSeconds=120 \
    -q -pl client clean package liberty:create liberty:install-feature liberty:deploy

./mvnw -ntp -pl server liberty:start
./mvnw -ntp -pl client liberty:start
sleep 10
./mvnw -ntp -pl server failsafe:integration-test liberty:stop
./mvnw -ntp -pl server failsafe:verify
./mvnw -ntp -pl client failsafe:integration-test liberty:stop
./mvnw -ntp -pl client failsafe:verify
