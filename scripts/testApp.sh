#!/bin/bash
set -euxo pipefail
./mvnw -version

if [[ -f docker-compose.yml ]]; then
    docker compose -f docker-compose.yml up -d
fi

./mvnw -ntp -Dhttp.keepAlive=false \
    -Dmaven.wagon.http.pool=false \
    -Dmaven.wagon.httpconnectionManager.ttlSeconds=120 \
    -q clean package liberty:create liberty:install-feature liberty:deploy

./mvnw -ntp liberty:start
sleep 20
./mvnw -ntp failsafe:integration-test liberty:stop
./mvnw -ntp failsafe:verify

if [[ -f docker-compose.yml ]]; then
    docker compose -f docker-compose.yml down
fi
