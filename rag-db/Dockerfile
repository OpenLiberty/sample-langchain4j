FROM icr.io/appcafe/open-liberty:full-java21-openj9-ubi-minimal as staging

# Define the variables for the OpenSSL subject.
ARG COUNTRY=CA
ARG STATE=Ontario
ARG LOCALITY=Markham
ARG ORG=IBM
ARG UNIT=OpenLiberty
ARG COMMON_NAME=localhost

# Generate a self-signed certificate and private key.
RUN openssl req -x509 -newkey rsa:2048 -nodes -days 365 \
    -out /tmp/cert.pem \
    -keyout /tmp/private.key \
    -subj "/C=$COUNTRY/ST=$STATE/L=$LOCALITY/O=$ORG/OU=$UNIT/CN=$COMMON_NAME"

# Combine the self-signed certificate and private key.
RUN cat /tmp/private.key /tmp/cert.pem > /tmp/mongodb_tls.pem

# Import combined file to truststore.
RUN keytool -import -trustcacerts -keystore /tmp/truststore.p12 \
    -storepass mongodb -storetype PKCS12 -alias mongo -file /tmp/cert.pem -noprompt

FROM mongodb/mongodb-atlas-local:latest

# Create the directories.
RUN mkdir -p /data/db/home/mongodb && \
    chown -R mongod:mongod /data/db/home/mongodb && \
    chmod -R u+w /data/db/home/mongodb
USER mongod

RUN mkdir /data/db/home/mongodb/data
RUN mkdir /data/db/home/mongodb/certs
RUN mkdir /data/db/home/mongodb/logs
RUN chown -R mongod:mongod /data/db/home/mongodb

# Copy the certificates over from stage one.
COPY --from=staging /tmp /data/db/home/mongodb/certs

# Copy the configuration and setup files from
# the host machine to the image.
COPY rag-db/mongodb.conf /data/db/home/mongodb
COPY rag-db/index.js /data/db/home/mongodb

# Run MongodDB daemon and execute the setup script.
RUN mongod \
        --fork \
        --config /data/db/home/mongodb/mongodb.conf \
    && mongosh \
        embeddingsdb \
        -tls \
        --tlsCAFile /data/db/home/mongodb/certs/cert.pem \
        --tlsCertificateKeyFile: /data/db/home/mongodb/certs/mongodb_tls.pem \
        --host localhost \
        /data/db/home/mongodb/index.js \
    && mongod --dbpath /data/db/home/mongodb/data --shutdown
# Start MongoDB daemon when the image is run in a container.
CMD ["mongod", "--config", "/home/mongodb/mongodb.conf"]