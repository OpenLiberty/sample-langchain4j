docker compose -f rag-db/docker-compose.yml down
docker compose -f rag-db/docker-compose.yml up -d

cd rag-db
./mvnw clean
./mvnw liberty:dev