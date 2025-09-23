# Open Liberty MCP StackOverflow Server Sample

This sample demonstrates how to build a Model Context Protocol (MCP) server on Open Liberty using Jakarta Servlet and the [MCP Java SDK](https://github.com/modelcontextprotocol/java-sdk). It implements a Stack Overflow service and exposes MCP tools for listing top questions and running free-text searches.
## Overview

The sample showcases:

* A Jakarta Servlet–based MCP server running on Open Liberty
* MCP [Streamable HTTP transport](https://modelcontextprotocol.io/specification/2025-06-18/basic/transports#streamable-http)
* Four Stack Overflow tools:
  * Top Jakarta EE questions and answers
  * Top MicroProfile questions and answers
  * Top LangChain4j questions and answers
  * Free-text Stack Overflow search

## Prerequisites and setup

To run this sample, you'll need [JDK 21](https://developer.ibm.com/languages/java/semeru-runtimes/downloads) or later.

Run the following command to set the `JAVA_HOME` environment variable:

```bash
export JAVA_HOME=<your Java 21 home path>
```

Navigate to the `sample-langchain4j/mcp/server` directory:

```bash
cd sample-langchain4j/mcp/server
```

## Start the application

Use the Maven wrapper to start the application by using the [Liberty dev mode](https://openliberty.io/docs/latest/development-mode.html):

```bash
./mvnw liberty:dev
```

When dev mode is ready, the server listens at http://localhost:9081/mcp, a single MCP endpoint that supports both POST and GET methods.

## Available Tools

### Jakarta EE Top Questions Tool
- **Name:** stackoverflow-jakarta-ee-top
- **Description:** Retrieves top Stack Overflow Q&A snippets tagged 'jakarta-ee'
- **Parameters:** None
- **Example:**
  ```java
  CallToolResult jakartaResult = client.callTool(
      new CallToolRequest("stackoverflow-jakarta-ee-top", Map.of())
  );
  ```

### MicroProfile Top Questions Tool
- **Name:** stackoverflow-microprofile-top  
- **Description:** Retrieves top Stack Overflow Q&A snippets tagged 'microprofile'  
- **Parameters:** None
- **Example:**
  ```java
  CallToolResult microprofileResult = client.callTool(
      new CallToolRequest("stackoverflow-microprofile-top", Map.of())
  );
  ```

### LangChain4j Top Questions Tool
- **Name:** stackoverflow-langchain4j-top  
- **Description:** Retrieves top Stack Overflow Q&A snippets tagged 'langchain4j'
- **Parameters:** None
- **Example:**
  ```java
  CallToolResult langchainResult = client.callTool(
      new CallToolRequest("stackoverflow-langchain4j-top", Map.of())
  );
  ```

### StackOverflow Search Tool
- **Name:** stackoverflow-search  
- **Description:** Searches Stack Overflow and returns the highest-voted answers for relevant questions  
- **Parameters:**
  - `query`: String - Free-text search query  
- **Example:**
  ```java
  CallToolResult searchResult = client.callTool(
      new CallToolRequest("stackoverflow-search", Map.of("query", "Open Liberty CDI"))
  );
  ```

## Running the tests

Because you started Liberty in dev mode, you can run the provided tests by pressing the `enter/return` key from the command-line session where you started dev mode.

If the tests pass, you see a similar output to the following example:

```bash
-------------------------------------------------------
 T E S T S
-------------------------------------------------------
Running it.io.openliberty.sample.server.McpServerIT
...
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.797 s...

Results:

Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
```

When you are done checking out the service, exit dev mode by pressing `Ctrl+C` in the command-line session where you ran Liberty, or by typing `q` and then pressing the `enter/return` key.
