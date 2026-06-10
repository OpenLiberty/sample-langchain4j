# Using RAG with LangChain4j in a Jakarta EE and MicroProfile Application

This example demonstrates RAG with LangChain4J in a Jakarta EE / MicroProfile application on Open Liberty. The application is a chatbot built with LangChain4J and uses MongoDB Atlas locally; it also uses Jakarta CDI, Jakarta RESTful Web Services, Jakarta WebSocket, MicroProfile Config, MicroProfile Metrics, and MicroProfile OpenAPI features. The application allows to use any model such as Github, Ollama or Mistral AI.

## Prerequisites:

1. [Java 21](https://developer.ibm.com/languages/java/semeru-runtimes/downloads)
2. Pull the Embedding Model from Ollama:
    1.&nbsp; Download and install [Ollama](https://ollama.com/download).
    2.&nbsp; Pull the following embedding model: `ollama pull nomic-embed-text`
    3.&nbsp; Ensure that Ollama is running: curl http://localhost:11434
3. Any one of the following chat model providers:
   - GitHub
        1.&nbsp; Sign up and sign in to https://github.com.
        2.&nbsp; Go to your [Settings/Developer Settings/Personal access tokens](https://github.com/settings/tokens).
        3.&nbsp; Generate a new token.
   - Ollama
        1.&nbsp; Pull the following model: `ollama pull llama3.2`
   - Mistral AI
        1.&nbsp; Sign up and log in to https://console.mistral.ai/home.
        2.&nbsp; Go to [Your API keys](https://console.mistral.ai/api-keys).
        3.&nbsp; Create a new key.
4. You will use Docker to run an instance of MongoDB for a fast installation and setup. Install Docker by following the instructions in the official [Docker documentation](https://docs.docker.com/engine/installation), and start your Docker environment.

## Setting up MongoDB Atlas Locally

This guide uses Docker to run an instance of MongoDB Atlas.

For more information about the `mongodb/mongodb-atlas-local` image, see [mongodb/mongodb-atlas-local](https://hub.docker.com/r/mongodb/mongodb-atlas-local) in Docker Hub.

### Running MongoDB in a Docker container

To run MongoDB in this example application, navigate to the `sample-langchain4j/rag-db` directory:

```
cd sample-langchain4j/rag-db
```

Run the following command to start the MongoDB Atlas:

```
docker compose -f docker-compose.yml up -d
```


## Environment Set Up

Set the `ENCRYPTION_KEY` environment variable for password encryption using the provided key. In production environments, you may manage and rotate encryption keys using enterprise key management solutions such as:

- **IBM Security Guardium Key Lifecycle Manager (GKLM)** 
- **IBM Cloud Secrets Manager** 
- **IBM Cloud Key Protect**
- **HashiCorp Vault**

```
export ENCRYPTION_KEY=customEncryptionKey1
```

Set the `JAVA_HOME` environment variable:

```
export JAVA_HOME=<your Java 21 home path>
```

Set the `GITHUB_API_KEY` environment variable if using Github as the chat model.

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

If you are currently using one of the following model providers: GitHub, Ollama or MistralAI, you may proceed.

Visit and try out the chat application at http://localhost:9081/.
  - At the prompt, try the following message examples:
    - ```
      Explain the Core Profile and Jakarta EE JSON Binding?
      ```
    - ```
      What are the default ConfigSources and the values? List in the order of default precedence.
      ```
  - Currently, the chat application does not use the knowledge base.

Navigate to http://localhost:9081/openapi/ui/ to see the OpenAPI user interface (UI) that provides API documentation and a client to test the API endpoints for MongoDB.
  - To try a particular api, authentication is required. The admin (read/write full access) and user (read only access) security roles are created.
    - For admin access, use `bob` and his password is `bobpwd`.
    - For read only access, use `alice` and her password is`alicepwd`.
  - Information about the REST APIs: 
    - The GET request at `/api/embedding` retrieves the content stored in the database.
    - The POST request at `/api/embedding` adds content to the database.
    - The POST request at `/api/embedding/init` processes and adds the knowledge base from `sample-langchain4j/rag/src/main/resources/knowledge_base`.
    - The PUT request at `/api/embedding/{id}` updates the content and summary given the id.
    - The DELETE request at `/api/embedding/{id}` removes the content from the database given the id.

Try the POST request at `/api/embedding/init` that adds the knowledge base embeddings to MongoDB.

Navigate to http://localhost:9081/ to try out the chat application again. Now, the application uses the knowledge base through RAG.
  - At the prompt, try the following message examples:
    - ```
      Explain the Core Profile and Jakarta EE JSON Binding?
      ```
    - ```
      What are the default ConfigSources and the values? List in the order of default precedence.
      ```

You can compare the AI responses to the knowledge base files at the `sample-langchain4j/rag-db/src/main/resources/knowledge_base` directory. The response should be more specific and relevant compared to the responses before.

You can try out adding your own data into the database directly by using POST `/api/embedding` API and then try out messages in RAG chat application.

Note that the embeddings and content that are stored in MongoDB previously are preserved even after restarting the application by stopping and running `./mvnw liberty:dev` again.

## Running the tests

Because you started Liberty in dev mode, you can run the provided tests by pressing the `enter/return` key from the command-line session where you started dev mode.

If the tests pass, you may see a similar output to the following example:

```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running it.io.openliberty.sample.langchan4j.RAGChatServiceIT
[INFO] ...
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 4.329 s -- in it.io.openliberty.sample.langchan4j.RAGChatServiceIT
[INFO] ...
[INFO] Running it.io.openliberty.sample.langchan4j.EmbeddingServiceIT
[INFO] ...
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.668 s -- in it.io.openliberty.sample.langchan4j.EmbeddingServiceIT
[INFO]
[INFO] Results:
[INFO]
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
```

When you are done checking out the service, exit dev mode by pressing `Ctrl+C` in the command-line session where you ran Liberty, or by typing `q` and then pressing the `enter/return` key.

Then, run the following command to stop and remove the container: 

```
docker compose -f docker-compose.yml down
```

