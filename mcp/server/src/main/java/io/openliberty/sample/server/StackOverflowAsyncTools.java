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
import io.modelcontextprotocol.server.McpServerFeatures.AsyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult.Builder;
import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@ApplicationScoped
public class StackOverflowAsyncTools {

    @Inject
    private StackOverflowService stackOverflowService;

    /** Tool: top Jakarta EE Q&A (no args) */
    public AsyncToolSpecification jakartaEETop() {
        return noArgsTool(
            "stackoverflow-jakarta-ee-top",
            "Stack Overflow: Jakarta EE (top)",
            "Retrieves top Stack Overflow Q&A snippets tagged 'jakarta-ee'.",
            stackOverflowService::searchJakartaEEQuestions
        );
    }

    /** Tool: top MicroProfile Q&A (no args) */
    public AsyncToolSpecification microProfileTop() {
        return noArgsTool(
            "stackoverflow-microprofile-top",
            "Stack Overflow: MicroProfile (top)",
            "Retrieves top Stack Overflow Q&A snippets tagged 'microprofile'.",
            stackOverflowService::searchMicroProfileQuestions
        );
    }

    /** Tool: top LangChain4j Q&A (no args) */
    public AsyncToolSpecification langChain4jTop() {
        return noArgsTool(
            "stackoverflow-langchain4j-top",
            "Stack Overflow: LangChain4j (top)",
            "Retrieves top Stack Overflow Q&A snippets tagged 'langchain4j'.",
            stackOverflowService::searchLangChain4jQuestions
        );
    }

    /** Tool: free-text Stack Overflow search (query: string) */
    public AsyncToolSpecification search() {

        JsonSchema inputJsonSchema =
            new JsonSchema(
                "object",
                Map.of(
                    "query", Map.of("type", "string")
                ),
                List.of("query"),
                null,
                null,
                null
            );

        Tool tool = Tool.builder()
            .name("stackoverflow-search")
            .title("Stack Overflow: Search")
            .description("Searches Stack Overflow and returns the highest-voted answers for relevant questions.")
            .inputSchema(inputJsonSchema)
            .build();

        return AsyncToolSpecification.builder()
            .tool(tool)
            .callHandler((McpAsyncServerExchange exchange, CallToolRequest request) ->
                Mono.fromCallable(() -> {
                    String query = (String) request.arguments().get("query");
                    if (query == null || query.isBlank()) {
                        return CallToolResult.builder()
                            .addTextContent("Missing required parameter: 'query'")
                            .isError(true)
                            .build();
                    }

                    List<String> output = stackOverflowService.searchStackOverflow(query);
                    Builder resultBuilder = CallToolResult.builder().isError(false);
                    if (output == null || output.isEmpty()) {
                        resultBuilder.addTextContent("No results.");
                    } else {
                        resultBuilder.textContent(output);
                    }
                    return resultBuilder.build();
                }).onErrorResume(error ->
                    Mono.just(
                        CallToolResult.builder()
                            .addTextContent("StackOverflow search failed: " + error.getMessage())
                            .isError(true)
                            .build()
                    )
                )
            )
            .build();
    }

    /** Helper to build a no-args async tool **/
    private AsyncToolSpecification noArgsTool(
        String toolName,
        String toolTitle,
        String toolDescription,
        Supplier<List<String>> toolLogic) {

        JsonSchema emptyJsonSchema = new JsonSchema(
            "object", Collections.emptyMap(), null, null, null, null);

        Tool toolDefinition = Tool.builder()
            .name(toolName)
            .title(toolTitle)
            .description(toolDescription)
            .inputSchema(emptyJsonSchema)
            .build();

        return AsyncToolSpecification.builder()
            .tool(toolDefinition)
            .callHandler((McpAsyncServerExchange exchange, CallToolRequest request) ->
                Mono.fromCallable(() -> {
                    List<String> output = toolLogic.get();
                    Builder resultBuilder = CallToolResult.builder().isError(false);
                    if (output == null || output.isEmpty()) {
                        resultBuilder.addTextContent("No results.");
                    } else {
                        resultBuilder.textContent(output);
                    }
                    return resultBuilder.build();
                }).onErrorResume(error ->
                    Mono.just(
                        CallToolResult.builder()
                            .addTextContent("Operation failed: " + error.getMessage())
                            .isError(true)
                            .build()
                    )
                )
            )
            .build();
    }
}
