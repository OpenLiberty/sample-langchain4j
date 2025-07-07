package io.openliberty.sample.langchain4j;

import java.util.logging.Logger;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class StackOverflowTools {

    private static String stackOverflowSite = "https://api.stackexchange.com/2.3/search/excerpts?";
    private static String size = "pagesize=3";
    
    private static String stackOverflowJakartaEE = stackOverflowSite + size + "&order=desc&sort=activity&tagged=jakarta-ee&site=stackoverflow";
    private static String stackOverflowMicroProfile = stackOverflowSite + size + "&order=desc&sort=activity&tagged=microprofile&site=stackoverflow";
    private static String stackOverflowLangChain4j = stackOverflowSite + size + "&order=desc&sort=activity&tagged=langchain4j&site=stackoverflow";

    private static String stackOverflowMethod = stackOverflowSite + size + "&order=desc&sort=activity";

    private static Logger logger = Logger.getLogger(ChatService.class.getName());

    private String clientSearch(String url) {
        Client client = ClientBuilder.newClient();
        Response response = client.target(url).request().get();
        String responseBody = response.readEntity(String.class);
        client.close();
        return responseBody;
    }

    @Tool("Multiple JakartaEE Questions on stackoverflow")
    public String searchJakartaEEQuestions() {
        logger.info("AI is searching stackoverflow for JakartaEE");
        return clientSearch(stackOverflowJakartaEE);
    }

    @Tool("Multiple MicroProfile Questions on stackoverflow")
    public String searchMicroProfileQuestions() {
        logger.info("AI is searching stackoverflow for MicroProfile");
        return clientSearch(stackOverflowMicroProfile);
    }

    @Tool("Multiple LangChain4j Questions on stackoverflow")
    public String searchLangChain4jQuestions() {
        logger.info("AI is searching stackoverflow for langchain4j");
        return clientSearch(stackOverflowLangChain4j);
    }

    @Tool("Search for questions and answers on stackoverflow")
    public String searchStackOverflow(@P("Question you are searching") String question) {
        logger.info("AI called the searchStackOverflow Tool with question: " + question);
        String targetUrl = stackOverflowMethod + "&q=" + question + "&site=stackoverflow";
        return clientSearch(targetUrl);
    }

}