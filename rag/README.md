# LangChain4j in Jakarta EE and MicroProfile

This example demonstrates LangChain4J in a Jakarta EE / MicroProfile application on Open Liberty. The application is a chatbot built with LangChain4J and uses Jakarta CDI, Jakarta RESTful Web Services, Jakarta WebSocket, MicroProfile Config, MicroProfile Metrics, and MicroProfile OpenAPI features. The application allows to use models from either Github, Ollama, or Hugging Face.

## Prerequisites:

- [Java 21](https://developer.ibm.com/languages/java/semeru-runtimes/downloads)
- Either one of the following model providers: - Github - Sign up and sign in to https://github.com. - Go to your [Settings](https://github.com/settings/profile)/[Developer Settings](https://github.com/settings/developers)/[Persional access tokens](https://github.com/settings/personal-access-tokens). - Generate a new token - Ollama - Download and install [Ollama](https://ollama.com/download) - see the [README.md](https://github.com/ollama/ollama/blob/main/README.md#ollama) - Pull the following model - `ollama pull llama3.2` - Mistral AI - Sign up and log in to https://console.mistral.ai/home. - Go to [Your API keys](https://console.mistral.ai/api-keys). - Create a new key. - Hugging Face - Sign up and log in to https://huggingface.co. - Go to [Access Tokens](https://huggingface.co/settings/tokens). - Create a new access token with `read` role.
- You will use Docker to run an instance of MongoDB for a fast installation and setup. Install Docker by following the instructions in the official [Docker documentation](https://docs.docker.com/engine/installation), and start your Docker environment.

## Setting up MongoDB

This guide uses Docker to run an instance of MongoDB. A multi-stage Dockerfile is provided for you. This Dockerfile uses the `mongo` image as the base image of the final stage and gathers the required configuration files. The resulting `mongo` image runs in a Docker container, and you must set up a new database for the microservice. Lastly, the truststore that's generated in the Docker image is copied from the container and placed into the Open Liberty configuration.

You can find more details and configuration options on the [MongoDB website](https://docs.mongodb.com/manual/reference/configuration-options/). For more information about the `mongo` image, see [mongo](https://hub.docker.com/_/mongo) in Docker Hub.

### Running MongoDB in a Docker container

To run MongoDB in this example application, navigate to the `sample-langchain4j` directory:

```
cd sample-langchain4j
```

Run the following commands to use the Dockerfile to build the image:

```
docker build -t mongo-embeddings -f rag-db/Dockerfile .
```

Run the image in a Docker container and map port `27017` from the container to your host machine:

```
docker run --name mongo-embeddings --rm -p 27017:27017 -d mongo-embeddings
```

### Adding the truststore to the Open Liberty configuration

The truststore that's created in the container needs to be added to the Open Liberty configuration so that the Liberty can trust the certificate that MongoDB presents when they connect. Run the following command to copy the `truststore.p12` file from the container to the `rag` directory:

```
docker cp \
  mongo-embeddings:/home/mongodb/certs/truststore.p12 \
  rag/src/main/liberty/config/resources/security
```

## Environment Set Up

To run this example application, navigate to the `sample-langchain4j/rag` directory:

```
cd sample-langchain4j/rag
```

Set the `JAVA_HOME` environment variable:

```
export JAVA_HOME=<your Java 21 home path>
```

Set the `GITHUB_API_KEY` environment variable if using Github.

```
unset OLLAMA_BASE_URL
unset MISTRAL_AI_API_KEY
export GITHUB_API_KEY=<your Github API token>
```

Set the `OLLAMA_BASE_URL` environment variable if using Ollama. Use your Ollama URL if not using the default.

```
unset GITHUB_API_KEY
unset MISTRAL_AI_API_KEY
export OLLAMA_BASE_URL=http://localhost:11434
```

Set the `MISTRAL_AI_API_KEY` environment variable if using Mistral AI.

```
unset GITHUB_API_KEY
unset OLLAMA_BASE_URL
export MISTRAL_AI_API_KEY=<your Mistral AI API key>
```

## Start the application

Use the Maven wrapper to start the application by using the [Liberty dev mode](https://openliberty.io/docs/latest/development-mode.html):

```
./mvnw liberty:dev
```

## Try out the application

If you are currently using one of the following model providers: GitHub, Ollama or MistralAI, you may proceed

- Navigate to http://localhost:9080/ to use the chat application

- At the prompt, try the following message examples:

  - ```
      Create a Java class that uses the MicroProfile Health API to check/montior if the CPU usage is below 95%.
    ```
  - ```
      How to enable the Jakarta EE Web Profile in Open Liberty?
    ```
  - ```
      How to chat with the assistant using langchain4j?
    ```

Use the `sample-langchain4j/rag/src/main/resources/knowledge_base` files to compare the AI responses to the provided files.


- Navigate to http://localhost:9080/openapi/ui/ to see the OpenAPI user interface (UI) that provides API documentation and a client to test the API endpoints for mongoDB.
  - To try a particular api, authentication is required.
  - The admin (read/write full access) and user (read only access) security roles are created. 
  - For full admin access: username = bob , password = bobpwd 
  - For read only access: username = alice , password = alicepwd
  - Example: Bob is a member of group admin and Alice is a member of group user.

After visiting the chat application at http://localhost:9080/, select the 'GET' request in the http://localhost:9080/openapi/ui/. The content from the `sample-langchain4j/rag/src/main/resources/knowledge_base` should be displayed.
  
## Running the tests

Because you started Liberty in dev mode, you can run the provided tests by pressing the `enter/return` key from the command-line session where you started dev mode.

If the tests pass, you see a similar output to the following example:

```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running it.dev.langchan4j.example.ToolServiceIT
[INFO] ...
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 10.14 s...
[INFO] Results:
[INFO]
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
```

When you are done checking out the service, exit dev mode by pressing `Ctrl+C` in the command-line session where you ran Liberty, or by typing `q` and then pressing the `enter/return` key.
