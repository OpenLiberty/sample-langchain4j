# LangChain4j in Jakarta EE and MicroProfile

This example demonstrates LangChain4j in a Jakarta EE / MicroProfile application on Open Liberty. The application is a chatbot built with LangChain4j and uses Jakarta CDI, Jakarta RESTful Web Services, Jakarta WebSocket, MicroProfile Config, MicroProfile Metrics, and MicroProfile OpenAPI features. The application can use models from either GitHub, Ollama, Mistral AI or Google AI.

## Prerequisites:

-   [Java 21](https://developer.ibm.com/languages/java/semeru-runtimes/downloads)
-   Any one of the following model providers:
    -   GitHub
        -   Sign up and sign in to https://github.com.
        -   Go to your [Settings](https://github.com/settings/profile)/[Developer Settings](https://github.com/settings/developers)/[Persional access tokens](https://github.com/settings/personal-access-tokens).
        -   Generate a new token
    -   Ollama
        -   Download and install [Ollama](https://ollama.com/download)
            -   see the [README.md](https://github.com/ollama/ollama/blob/main/README.md#ollama)
        -   Pull the following model
            -   `ollama pull llama3.2`
    -   Mistral AI
        -   Sign up and log in to https://console.mistral.ai/home.
        -   Go to [Your API keys](https://console.mistral.ai/api-keys).
        -   Create a new key.
    -   Google AI
        -   Sign up and log in to https://aistudio.google.com.
        -   Click [Get API key](https://aistudio.google.com/apikey).
        -   Create a new API key.

## Environment Set Up

To run this example application, navigate to the `sample-langchain4j/tools` directory:

```
cd sample-langchain4j/tools
```

Set the `JAVA_HOME` environment variable:

```
export JAVA_HOME=<your Java 21 home path>
```

Set the `GITHUB_API_KEY` environment variable if using GitHub.

```
unset GEMINI_AI_API_KEY
unset OLLAMA_BASE_URL
unset MISTRAL_AI_API_KEY
export GITHUB_API_KEY=<your GitHub API token>
```

Set the `OLLAMA_BASE_URL` environment variable if using Ollama. Use your Ollama URL if not using the default.

```
unset GEMINI_AI_API_KEY
unset GITHUB_API_KEY
unset MISTRAL_AI_API_KEY
export OLLAMA_BASE_URL=http://localhost:11434
```

Set the `MISTRAL_AI_API_KEY` environment variable if using Mistral AI.

```
unset GEMINI_AI_API_KEY
unset GITHUB_API_KEY
unset OLLAMA_BASE_URL
export MISTRAL_AI_API_KEY=<your Mistral AI API key>
```

Set the `GEMINI_AI_API_KEY` environment variable if using Google AI.

```
unset GITHUB_API_KEY
unset OLLAMA_BASE_URL
unset MISTRAL_AI_API_KEY
export GEMINI_AI_API_KEY=<your Google AI API key>
```

## Start the application

Use the Maven wrapper to start the application by using the [Liberty dev mode](https://openliberty.io/docs/latest/development-mode.html):

```
./mvnw liberty:dev
```

## Try out the application

- Navigate to http://localhost:9080/toolChat.html
- At the prompt, try the following message examples:
  - ```
    What are some current problems users have with LangChain4j?
    ```
  - ```
    What are some current problems users have when using Ollama Face?
    ```

## Running the tests

Because you started Liberty in dev mode, you can run the provided tests by pressing the `enter/return` key from the command-line session where you started dev mode.

If the tests pass, you see a similar output to the following example:

```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running it.io.openliberty.sample.langchain4j.ToolServiceIT
[INFO] ...
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 10.14 s...
[INFO] Results:
[INFO] 
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
```

When you are done checking out the service, exit dev mode by pressing `Ctrl+C` in the command-line session where you ran Liberty, or by typing `q` and then pressing the `enter/return` key.
