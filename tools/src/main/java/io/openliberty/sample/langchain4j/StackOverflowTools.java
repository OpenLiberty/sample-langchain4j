/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.sample.langchain4j;

import java.util.logging.Logger;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class StackOverflowTools {
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

    private static Logger logger = Logger.getLogger(ChatService.class.getName());

    private Map<String, Object> stringToMap(String jsonString) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(jsonString, new TypeReference<Map<String, Object>>() {});
    }

    private List<Map<String, Object>> clientSearch(String url) throws Exception {
        Client client = ClientBuilder.newClient();
        Response response = client.target(url).request().get();
        String responseBody = response.readEntity(String.class);
        client.close();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> arrayData = (List<Map<String, Object>>) stringToMap(responseBody).get("items");

        return arrayData;
    }

    private ArrayList<String> questionAndAnswer(String url) throws Exception {
        ArrayList <String> questionAnswer = new ArrayList<>();
        for (Map<String, Object> data : clientSearch(url)) {
            String topAnswer = clientSearch(String.format(findAnswer, data.get("question_id")))
                                    .get(0).get("body").toString();
            questionAnswer.add(
                "Link: " + "https://stackoverflow.com/questions/" + data.get("question_id") + 
                " Problem: " + data.get("body") +
                " Answer: " + topAnswer
            );
        }
        return questionAnswer;
    }

    @Tool("Multiple JakartaEE Questions on stackoverflow")
    public ArrayList <String> searchJakartaEEQuestions() throws Exception {
        logger.info("AI is searching stackoverflow for JakartaEE");
        return questionAndAnswer(stackOverflowJakartaEE);
    }

    @Tool("Multiple MicroProfile Questions on stackoverflow")
    public ArrayList <String> searchMicroProfileQuestions() throws Exception {
        logger.info("AI is searching stackoverflow for MicroProfile");
        return questionAndAnswer(stackOverflowMicroProfile);
    }

    @Tool("Multiple LangChain4j Questions on stackoverflow")
    public ArrayList <String> searchLangChain4jQuestions() throws Exception {
        logger.info("AI is searching stackoverflow for langchain4j");
        
        return questionAndAnswer(stackOverflowLangChain4j);
    }

    @Tool("Search for questions and answers on stackoverflow")
    public ArrayList <String> searchStackOverflow(@P("Question you are searching") String question) throws Exception {
        logger.info("AI called the searchStackOverflow Tool with question: " + question);
        String targetUrl = stackOverflowMethod + "&q=" + question;
        return questionAndAnswer(targetUrl);
    }

}