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

import java.util.logging.Logger;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StackOverflowService {
    private static String stackOverflowSite = "https://api.stackexchange.com/2.3/search/excerpts?site=stackoverflow";
    private static String size = "&pagesize=3";
    private static String filter = "&filter=N9UNq*RI0tsSN35uJKC0nk_HM";
    
    private static String stackOverflowJakartaEE = stackOverflowSite + filter + size + "&order=desc&sort=relevance&tagged=jakarta-ee";
    private static String stackOverflowMicroProfile = stackOverflowSite + filter + size + "&order=desc&sort=relevance&tagged=microprofile";
    private static String stackOverflowLangChain4j = stackOverflowSite + filter + size + "&order=desc&sort=relevance&tagged=langchain4j";

    private static String stackOverflowMethod = stackOverflowSite + filter + size + "&order=desc&sort=relevance&answers=1";

    private static String findAnswer = "https://api.stackexchange.com"
                                       + "/2.3/questions/%s/answers?order=desc&sort=votes&site=stackoverflow&"
                                       + "filter=CKAkJFla(8TLNtkfr1ytJZj94MlNVo6Ee";

    private static Logger logger = Logger.getLogger(StackOverflowService.class.getName());

    private Map<String, Object> stringToMap(String jsonString) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(jsonString, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            logger.severe("Error parsing JSON: " + e.getMessage());
            return Map.of(); // return empty map
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
            logger.severe("Error fetching data from StackOverflow API: " + e.getMessage());
        }
        return arrayData;
    }

    private ArrayList<String> questionAndAnswer(String url) {
        ArrayList<String> questionAnswer = new ArrayList<>();
        try {
            for (Map<String, Object> data : clientSearch(url)) {
                try {
                    String topAnswer = clientSearch(String.format(findAnswer, data.get("question_id")))
                                            .get(0).get("body").toString();
                    String qId   = String.valueOf(data.get("question_id"));
                    String qUrl   = "https://stackoverflow.com/questions/" + qId;
                    String title = String.valueOf(data.getOrDefault("title", ""));
                    String body  = String.valueOf(data.get("body"));

                    String line =
                    "- [" + escapeMd(title) + "](" + qUrl + ")\n" +
                    "  Problem: " + stripHtml(body) + "\n" +
                    "  Top answer: " + stripHtml(topAnswer);

                    questionAnswer.add(line);
                } catch (Exception e) {
                    logger.warning("Error fetching answer for question " + data.get("question_id") + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.severe("Error processing questions: " + e.getMessage());
        }
        return questionAnswer;
    }

    public ArrayList<String> searchJakartaEEQuestions() {
        logger.info("AI is searching stackoverflow for JakartaEE");
        return questionAndAnswer(stackOverflowJakartaEE);
    }

    public ArrayList<String> searchMicroProfileQuestions() {
        logger.info("AI is searching stackoverflow for MicroProfile");
        return questionAndAnswer(stackOverflowMicroProfile);
    }

    public ArrayList<String> searchLangChain4jQuestions() {
        logger.info("AI is searching stackoverflow for langchain4j");
        return questionAndAnswer(stackOverflowLangChain4j);
    }

    public ArrayList<String> searchStackOverflow(String question) {
        logger.info("AI called the searchStackOverflow Tool with question: " + question);
        String targetUrl = stackOverflowMethod + "&q=" + question;
        return questionAndAnswer(targetUrl);
    }

    private static String stripHtml(String s) {
        if (s == null) return "";
        // preserve code blocks a bit
        s = s.replaceAll("(?is)<pre><code>(.*?)</code></pre>", "\n```\n$1\n```\n");
        // strip tags
        s = s.replaceAll("(?is)<[^>]+>", "");
        // collapse whitespace
        return s.replaceAll("[ \\t\\x0B\\f\\r]+", " ").replaceAll("\\n{3,}", "\n\n").trim();
    }

    private static String escapeMd(String s) {
        if (s == null) return "";
        return s.replace("[", "\\[").replace("]", "\\]").replace("(", "\\(").replace(")", "\\)");
    }
}
