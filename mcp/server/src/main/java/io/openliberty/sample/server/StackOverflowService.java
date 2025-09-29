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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

@ApplicationScoped
public class StackOverflowService {

    private static final Logger LOGGER = Logger.getLogger(StackOverflowService.class.getName());

    private static final String STACK_EXCERPTS = "https://api.stackexchange.com/2.3/search/excerpts?site=stackoverflow";
    private static final String SIZE = "&pagesize=3";
    private static final String FILTER = "&filter=N9UNq*RI0tsSN35uJKC0nk_HM";

    private static final String JAKARTA_EE_URL    = STACK_EXCERPTS + FILTER + SIZE + "&order=desc&sort=relevance&tagged=jakarta-ee";
    private static final String MICROPROFILE_URL  = STACK_EXCERPTS + FILTER + SIZE + "&order=desc&sort=relevance&tagged=microprofile";
    private static final String LANGCHAIN4J_URL   = STACK_EXCERPTS + FILTER + SIZE + "&order=desc&sort=relevance&tagged=langchain4j";
    private static final String SEARCH_BASE       = STACK_EXCERPTS + FILTER + SIZE + "&order=desc&sort=relevance&answers=1";

    private static final String FIND_ANSWER_FMT = "https://api.stackexchange.com"
        + "/2.3/questions/%s/answers?order=desc&sort=votes&site=stackoverflow&"
        + "filter=CKAkJFla(8TLNtkfr1ytJZj94MlNVo6Ee";

    private Map<String, Object> stringToMap(String json) {
        try {
            return new ObjectMapper().readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            LOGGER.severe("Error parsing JSON: " + e.getMessage());
            return Map.of();
        }
    }

    private List<Map<String, Object>> clientSearch(String url) {
        List<Map<String, Object>> arrayData = new ArrayList<>();
        try {
            Client client = ClientBuilder.newClient();
            Response response = client.target(url).request().get();
            String responseBody = response.readEntity(String.class);
            client.close();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) stringToMap(responseBody).get("items");
            if (items != null) {
                arrayData = items;
            }
        } catch (Exception e) {
            LOGGER.severe("Error fetching data from StackOverflow API: " + e.getMessage());
        }
        return arrayData;
    }

    private List<Map<String,Object>> questionAndAnswer(String url) {
        List<Map<String,Object>> out = new ArrayList<>();
        try {
            for (Map<String, Object> data : clientSearch(url)) {
                String qId   = String.valueOf(data.get("question_id"));
                String title = escapeMd(String.valueOf(data.getOrDefault("title", "")));
                String body  = String.valueOf(data.get("body"));

                Map<String,Object> item = new LinkedHashMap<>();
                item.put("Title", title);
                item.put("URL", "https://stackoverflow.com/questions/" + qId);
                item.put("Problem description", stripHtml(body));
                try {
                    List<Map<String, Object>> answers = clientSearch(String.format(FIND_ANSWER_FMT, qId));
                    String topAnswer = answers.isEmpty() ? "No answers." : String.valueOf(answers.get(0).get("body"));
                    item.put("Top answer", stripHtml(topAnswer));
                    out.add(item);
                } catch (Exception e) {
                    LOGGER.warning("Error fetching answer for question " + qId + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            LOGGER.severe("Error processing questions: " + e.getMessage());
        }
        return out;
    }

    public List<Map<String,Object>> searchJakartaEEQuestions() {
        LOGGER.info("AI is searching stackoverflow for JakartaEE");
        return questionAndAnswer(JAKARTA_EE_URL);
    }

    public List<Map<String,Object>> searchMicroProfileQuestions() {
        LOGGER.info("AI is searching stackoverflow for MicroProfile");
        return questionAndAnswer(MICROPROFILE_URL);
    }

    public List<Map<String,Object>> searchLangChain4jQuestions() {
        LOGGER.info("AI is searching stackoverflow for langchain4j");
        return questionAndAnswer(LANGCHAIN4J_URL);
    }

    public List<Map<String,Object>> searchStackOverflow(String question) {
        LOGGER.info("AI called the searchStackOverflow tool with question: " + question);
        String targetUrl = SEARCH_BASE + "&q=" + question;
        return questionAndAnswer(targetUrl);
    }

    private static String stripHtml(String s) {
        if (s == null) return "";
        s = s.replaceAll("(?is)<pre><code>(.*?)</code></pre>", "\n```\n$1\n```\n");
        s = s.replaceAll("(?is)<[^>]+>", "");
        return s.replaceAll("[ \\t\\x0B\\f\\r]+", " ").replaceAll("\\n{3,}", "\n\n").trim();
    }

    private static String escapeMd(String s) {
        if (s == null) return "";
        return s.replace("[", "\\[").replace("]", "\\]").replace("(", "\\(").replace(")", "\\)");
    }
}
