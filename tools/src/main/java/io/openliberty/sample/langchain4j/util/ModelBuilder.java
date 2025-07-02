package io.openliberty.sample.langchain4j.util;

import static dev.langchain4j.model.github.GitHubModelsChatModelName.PHI_3_MINI_INSTRUCT_4K;
import static dev.langchain4j.model.github.GitHubModelsEmbeddingModelName.TEXT_EMBEDDING_3_SMALL;
import static dev.langchain4j.model.huggingface.HuggingFaceModelName.SENTENCE_TRANSFORMERS_ALL_MINI_LM_L6_V2;
import static dev.langchain4j.model.mistralai.MistralAiChatModelName.MISTRAL_SMALL_LATEST;
import static dev.langchain4j.model.mistralai.MistralAiEmbeddingModelName.MISTRAL_EMBED;
import static java.time.Duration.ofSeconds;

import java.util.logging.Logger;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.github.GitHubModelsChatModel;
import dev.langchain4j.model.huggingface.HuggingFaceChatModel;
import dev.langchain4j.model.mistralai.MistralAiChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ModelBuilder {

    private static Logger logger = Logger.getLogger(ModelBuilder.class.getName());

    @Inject
    @ConfigProperty(name = "hugging.face.api.key")
    private String HUGGING_FACE_API_KEY;

    @Inject
    @ConfigProperty(name = "hugging.face.language.model.id")
    private String HUGGING_FACE_LANGUAGE_MODEL_ID;

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

    private ChatModel chatModel = null;

    public boolean usingGithub() {
        return GITHUB_API_KEY.startsWith("ghp_");
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

    public ChatModel getChatModel() throws Exception {
        if (chatModel == null) {
            if (usingGithub()) {
                chatModel = GitHubModelsChatModel.builder()
                    .gitHubToken(GITHUB_API_KEY)
                    .modelName(GITHUB_CHAT_MODEL_ID)
                    .timeout(ofSeconds(TIMEOUT))
                    .temperature(TEMPERATURE)
                    .maxTokens(MAX_NEW_TOKEN)
                    .build();
                logger.info("using Github " + GITHUB_CHAT_MODEL_ID + " chat model for the web");
            } else if (usingOllama()) {
                chatModel = OllamaChatModel.builder()
                    .baseUrl(OLLAMA_BASE_URL)
                    .modelName(OLLAMA_CHAT_MODEL_ID)
                    .timeout(ofSeconds(TIMEOUT))
                    .temperature(TEMPERATURE)
                    .numPredict(MAX_NEW_TOKEN)
                    .build();
                logger.info("using Ollama " + OLLAMA_CHAT_MODEL_ID + " chat model for the web");
            } else if (usingMistralAi()) {
                chatModel = MistralAiChatModel.builder()
                    .apiKey(MISTRAL_AI_API_KEY)
                    .modelName(MISTRAL_AI_MISTRAL_CHAT_MODEL_ID)
                    .timeout(ofSeconds(TIMEOUT))
                    .temperature(TEMPERATURE)
                    .maxTokens(MAX_NEW_TOKEN)
                    .build();
                logger.info("using Mistral AI " + MISTRAL_AI_MISTRAL_CHAT_MODEL_ID + " chat model for the web");
            } else if (usingHuggingFace()) {
                chatModel = HuggingFaceChatModel.builder()
                    .accessToken(HUGGING_FACE_API_KEY)
                    .modelId(HUGGING_FACE_CHAT_MODEL_ID)
                    .timeout(ofSeconds(TIMEOUT))
                    .temperature(TEMPERATURE)
                    .maxNewTokens(MAX_NEW_TOKEN)
                    .waitForModel(true)
                    .build();
                logger.info("using Hugging Face " + HUGGING_FACE_CHAT_MODEL_ID + " chat model for the web");

            } else {
                throw new Exception("No available platform to access model");
            }
        }
        return chatModel;
    }
}
