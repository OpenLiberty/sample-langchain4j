package dev.langchain4j.example.util;

import static java.time.Duration.ofSeconds;

import java.util.logging.Logger;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.github.GitHubModelsStreamingChatModel;
import dev.langchain4j.model.mistralai.MistralAiStreamingChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ModelBuilder {

    private static Logger logger = Logger.getLogger(ModelBuilder.class.getName());

    @Inject
    @ConfigProperty(name = "hugging.face.api.key")
    private String HUGGING_FACE_API_KEY;

    @Inject
    @ConfigProperty(name = "hugging.face.chat.model.id")
    private String HUGGING_FACE_CHAT_MODEL_ID;

    @Inject
    @ConfigProperty(name = "github.api.key")
    private String GITHUB_API_KEY;

    @Inject
    @ConfigProperty(name = "github.chat.model.id")
    private String GITHUB_CHAT_MODEL_ID;

    @Inject
    @ConfigProperty(name = "ollama.base.url")
    private String OLLAMA_BASE_URL;

    @Inject
    @ConfigProperty(name = "ollama.chat.model.id")
    private String OLLAMA_CHAT_MODEL_ID;

    @Inject
    @ConfigProperty(name = "mistral.ai.api.key")
    private String MISTRAL_AI_API_KEY;

    @Inject
    @ConfigProperty(name = "mistral.ai.chat.model.id")
    private String MISTRAL_AI_MISTRAL_CHAT_MODEL_ID;

    @Inject
    @ConfigProperty(name = "chat.model.timeout")
    private Integer TIMEOUT;

    @Inject
    @ConfigProperty(name = "chat.model.max.token")
    private Integer MAX_NEW_TOKEN;

    @Inject
    @ConfigProperty(name = "chat.model.temperature")
    private Double TEMPERATURE;

    private StreamingChatModel streamingChatModel = null;

    public boolean usingGithub() {
        return GITHUB_API_KEY.startsWith("ghp_") || GITHUB_API_KEY.startsWith("github_pat_");
    }

    public boolean usingOllama() {
        return OLLAMA_BASE_URL.startsWith("http");
    }

    public boolean usingMistralAi() {
        return MISTRAL_AI_API_KEY.length() > 30;
    }

    public boolean usingHuggingFace() {
        return HUGGING_FACE_API_KEY.startsWith("hf_");
    }

    public StreamingChatModel getStreamingChatModel() throws Exception {
        if (streamingChatModel == null) {
            if (usingGithub()) {
                streamingChatModel = GitHubModelsStreamingChatModel.builder()
                    .gitHubToken(GITHUB_API_KEY)
                    .modelName(GITHUB_CHAT_MODEL_ID)
                    .timeout(ofSeconds(TIMEOUT))
                    .temperature(TEMPERATURE)
                    .maxTokens(MAX_NEW_TOKEN)
                    .build();
                logger.info("using Github " + GITHUB_CHAT_MODEL_ID + " streaming chat model for the web");
            } else if (usingOllama()) {
                streamingChatModel = OllamaStreamingChatModel.builder()
                    .baseUrl(OLLAMA_BASE_URL)
                    .modelName(OLLAMA_CHAT_MODEL_ID)
                    .timeout(ofSeconds(TIMEOUT))
                    .temperature(TEMPERATURE)
                    .numPredict(MAX_NEW_TOKEN)
                    .build();
                logger.info("using Ollama " + OLLAMA_CHAT_MODEL_ID + " streaming chat model for the web");
            } else if (usingMistralAi()) {
                streamingChatModel = MistralAiStreamingChatModel.builder()
                    .apiKey(MISTRAL_AI_API_KEY)
                    .modelName(MISTRAL_AI_MISTRAL_CHAT_MODEL_ID)
                    .timeout(ofSeconds(TIMEOUT))
                    .temperature(TEMPERATURE)
                    .maxTokens(MAX_NEW_TOKEN)
                    .build();
                logger.info("using Mistral AI " + MISTRAL_AI_MISTRAL_CHAT_MODEL_ID + " streaming chat model for the web");
            } else if (usingHuggingFace()) {
                throw new Exception("LangChain4J Hugging Face APIs do not support streaming chat model");
            } else {
                throw new Exception("No available platform to access model");
            }
        }
        return streamingChatModel;
    }

}
