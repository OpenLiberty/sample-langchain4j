/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.sample.server;

import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class StackOverflowAsyncTools {

    private final StackOverflowService stackOverflowService;

    public StackOverflowAsyncTools(StackOverflowService stackOverflowService) {
        this.stackOverflowService = Objects.requireNonNull(stackOverflowService);
    }

    /** Tool: top Jakarta EE Q&A (no args) */
    public McpServerFeatures.AsyncToolSpecification jakartaEETop() {
        return noArgsTool(
            "stackoverflow-jakarta-ee-top",
            "Stack Overflow: Jakarta EE (top)",
            "Retrieves top Stack Overflow Q&A snippets tagged 'jakarta-ee'.",
            stackOverflowService::searchJakartaEEQuestions
        );
    }

    /** Tool: top MicroProfile Q&A (no args) */
    public McpServerFeatures.AsyncToolSpecification microProfileTop() {
        return noArgsTool(
            "stackoverflow-microprofile-top",
            "Stack Overflow: MicroProfile (top)",
            "Retrieves top Stack Overflow Q&A snippets tagged 'microprofile'.",
            stackOverflowService::searchMicroProfileQuestions
        );
    }

    /** Tool: top LangChain4j Q&A (no args) */
    public McpServerFeatures.AsyncToolSpecification langChain4jTop() {
        return noArgsTool(
            "stackoverflow-langchain4j-top",
            "Stack Overflow: LangChain4j (top)",
            "Retrieves top Stack Overflow Q&A snippets tagged 'langchain4j'.",
            stackOverflowService::searchLangChain4jQuestions
        );
    }

    /** Tool: free-text Stack Overflow search (query: string) */
    public McpServerFeatures.AsyncToolSpecification search() {
        var inputSchemaJson = """
        {
          "type": "object",
          "id": "urn:jsonschema:StackOverflowSearch",
          "properties": {
            "query": { "type": "string", "description": "Free-text search query" }
          },
          "required": ["query"],
          "additionalProperties": false
        }
        """;

        var tool = McpSchema.Tool.builder()
            .name("stackoverflow-search")
            .title("Stack Overflow: Search")
            .description("Searches Stack Overflow and returns the highest-voted answers for relevant questions.")
            .inputSchema(inputSchemaJson)
            .build();

        return McpServerFeatures.AsyncToolSpecification.builder()
            .tool(tool)
            .callHandler((McpAsyncServerExchange exchange, McpSchema.CallToolRequest req) ->
                Mono.fromCallable(() -> {
                    String q = (String) req.arguments().get("query");
                    if (q == null || q.isBlank()) {
                        return McpSchema.CallToolResult.builder()
                            .addTextContent("Missing required parameter: 'query'")
                            .isError(true)
                            .build();
                    }

                    List<String> results = stackOverflowService.searchStackOverflow(q);

                    var b = McpSchema.CallToolResult.builder().isError(false);
                    if (results == null || results.isEmpty()) {
                        b.addTextContent("No results.");
                    } else {
                        b.textContent(results); // each String -> TextContent
                    }
                    return b.build();
                }).onErrorResume(e ->
                    Mono.just(
                        McpSchema.CallToolResult.builder()
                            .addTextContent("StackOverflow search failed: " + e.getMessage())
                            .isError(true)
                            .build()
                    )
                )
            )
            .build();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Helper to build a no-args async tool using Tool.builder() + AsyncToolSpecification.builder()
    // ─────────────────────────────────────────────────────────────────────────────
    private McpServerFeatures.AsyncToolSpecification noArgsTool(
            String name,
            String title,
            String description,
            Supplier<List<String>> impl
    ) {
        var emptySchemaJson = """
        {
          "type": "object",
          "properties": {},
          "required": [],
          "additionalProperties": false
        }
        """;

        var tool = McpSchema.Tool.builder()
            .name(name)
            .title(title)
            .description(description)
            .inputSchema(emptySchemaJson)
            .build();

        return McpServerFeatures.AsyncToolSpecification.builder()
            .tool(tool)
            .callHandler((McpAsyncServerExchange exchange, McpSchema.CallToolRequest req) ->
                Mono.fromCallable(() -> {
                    List<String> results = impl.get();
                    var b = McpSchema.CallToolResult.builder().isError(false);
                    if (results == null || results.isEmpty()) {
                        b.addTextContent("No results.");
                    } else {
                        b.textContent(results);
                    }
                    return b.build();
                }).onErrorResume(e ->
                    Mono.just(
                        McpSchema.CallToolResult.builder()
                            .addTextContent("Operation failed: " + e.getMessage())
                            .isError(true)
                            .build()
                    )
                )
            )
            .build();
    }
}
