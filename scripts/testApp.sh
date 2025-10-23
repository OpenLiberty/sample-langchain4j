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

if [[ "$1" == "rag-db" ]]; then
    sleep 5
    time_out=0
    while :
    do
        if [ "$(curl -s --user bob:bobpwd http://localhost:9081/api/embedding)" = "[]" ]; then
            echo Ready to test
            break
        fi
        time_out=$((time_out + 1))
        echo waiting $time_out ...
        sleep 5
        if [ "$time_out" = "24" ]; then
            echo Unable to run test
            exit 1
        fi
    done
else
    sleep 10
fi

./mvnw -ntp failsafe:integration-test liberty:stop
./mvnw -ntp failsafe:verify

if [[ -f docker-compose.yml ]]; then
    docker compose -f docker-compose.yml down
fi
