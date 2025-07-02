# LangChain4j in Jakarta EE and MicroProfile

This example demonstrates LangChain4J in a Jakarta EE / MicroProfile application on Open Liberty. The application is a chatbot built with LangChain4J and uses Jakarta CDI, Jakarta RESTful Web Services, Jakarta WebSocket, MicroProfile Config, MicroProfile Metrics, and MicroProfile OpenAPI features. The application allows to use models from either Github, Ollama, or Hugging Face.

## Prerequisites:

-   [Java 21](https://developer.ibm.com/languages/java/semeru-runtimes/downloads)
-   Either one of the following model providers:
    -   Github
        -   Sign up and sign in to https://github.com.
        -   Go to your [Settings](https://github.com/settings/profile)/[Developer Settings](https://github.com/settings/developers)/[Persional access tokens](https://github.com/settings/personal-access-tokens).
        -   Generate a new token
    -   Ollama
        -   Download and install [Ollama](https://ollama.com/download)
            -   see the [README.md](https://github.com/ollama/ollama/blob/main/README.md#ollama)
        -   Pull the following models
            -   `ollama pull llama3.2`
            -   `ollama pull all-minilm`
            -   `ollama pull tinydolphin`
    -   Mistral AI
        -   Sign up and log in to https://console.mistral.ai/home.
        -   Go to [Your API keys](https://console.mistral.ai/api-keys).
        -   Create a new key.
    -   Hugging Face
        -   Sign up and log in to https://huggingface.co.
        -   Go to [Access Tokens](https://huggingface.co/settings/tokens).
        -   Create a new access token with `read` role.

## Environment Set Up

To run this example application, navigate to the `sample-langchain4j` directory:

```
cd sample-langchain4j/tools
```

Set the `JAVA_HOME` environment variable:

```
export JAVA_HOME=<your Java 21 home path>
```

Set the `GITHUB_API_KEY` environment variable if using Github.

```
unset HUGGING_FACE_API_KEY
unset OLLAMA_BASE_URL
unset MISTRAL_AI_API_KEY
export GITHUB_API_KEY=<your Github API token>
```

Set the `OLLAMA_BASE_URL` environment variable if using Ollama. Use your Ollama URL if not using the default.

```
unset HUGGING_FACE_API_KEY
unset GITHUB_API_KEY
unset MISTRAL_AI_API_KEY
export OLLAMA_BASE_URL=http://localhost:11434
```

Set the `MISTRAL_AI_API_KEY` environment variable if using Mistral AI.

```
unset HUGGING_FACE_API_KEY
unset GITHUB_API_KEY
unset OLLAMA_BASE_URL
export MISTRAL_AI_API_KEY=<your Mistral AI API key>
```

Set the `HUGGING_FACE_API_KEY` environment variable if using Hugging Face.

```
unset GITHUB_API_KEY
unset OLLAMA_BASE_URL
unset MISTRAL_AI_API_KEY
export HUGGING_FACE_API_KEY=<your Hugging Face read token>
```

## Start the application

Use the Maven wrapper to start the application by using the [Liberty dev mode](https://openliberty.io/docs/latest/development-mode.html):

```
./mvnw liberty:dev
```

## Try out the application

If you are currently using one of the following model providers: GitHub, Ollama or MistralAI, you may proceed

- Navigate to http://localhost:9080/toolChat.html
- At the prompt, try the following message examples:
  - ```
    What are some current problems users have with LangChain4J?
    ```
  - ```
    What are some possible solutions to the problems?
    ```

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
