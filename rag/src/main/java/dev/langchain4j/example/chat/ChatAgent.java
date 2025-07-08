package dev.langchain4j.example.chat;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;

import dev.langchain4j.example.chat.util.ModelBuilder;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.injector.ContentInjector;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.content.retriever.WebSearchContentRetriever;
import dev.langchain4j.rag.query.router.DefaultQueryRouter;
import dev.langchain4j.rag.query.router.QueryRouter;

import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.mongodb.IndexMapping;
import dev.langchain4j.store.embedding.mongodb.MongoDbEmbeddingStore;

import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.tavily.TavilyWebSearchEngine;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.HashSet;
import java.util.Set;

import org.bson.conversions.Bson;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.model.CreateCollectionOptions;

import io.github.cdimascio.dotenv.Dotenv;

import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.ArrayList;
import java.util.zip.ZipFile;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import java.net.URL;

record Data(String fileName, String source, Date date) {
}

@ApplicationScoped
public class ChatAgent {

        @Inject
        private ModelBuilder modelBuilder;

        @Inject
        @ConfigProperty(name = "chat.memory.max.messages")
        private Integer MAX_MESSAGES;

        interface Assistant {
                String chat(@MemoryId String sessionId, @UserMessage String userMessage);
        }

        private Assistant assistant = null;

        public Assistant getAssistant() throws Exception {
                if (assistant == null) {

                        ChatModel model = modelBuilder.getChatModelForWeb();

                        URL zipUrl = getClass().getClassLoader().getResource("document_files/files.zip");
                        System.out.println(zipUrl);
                        File fileZip = new File(zipUrl.toURI());
                        ZipFile zipFile = new ZipFile(fileZip);

                        Enumeration<? extends ZipEntry> entries = zipFile.entries();
                        ArrayList<String> filesStr = new ArrayList<>();
                        // an array of records stores metadata for each file
                        ArrayList<Data> dataStore = new ArrayList<>();

                        while (entries.hasMoreElements()) {
                                ZipEntry entry = entries.nextElement();
                                if (!entry.isDirectory() && entry.getName().endsWith(".txt")
                                                && !entry.getName().startsWith("__MACOSX")) {
                                        System.out.println("File processing ... " + entry.getName());
                                        String textFile = " ";
                                        Date todaysDate = new Date();
                                        String fileName = entry.getName();
                                        Data elem = new Data(fileName, "src_default", todaysDate);
                                        dataStore.add(elem);
                                        try (InputStream inpStreamTxt = zipFile.getInputStream(entry);
                                                        BufferedReader reader = new BufferedReader(
                                                                        new InputStreamReader(inpStreamTxt,
                                                                                        StandardCharsets.UTF_8))) {
                                                String line = " ";
                                                while ((line = reader.readLine()) != null) {
                                                        textFile = textFile + line;
                                                }
                                        }

                                        filesStr.add(textFile);

                                } else if (!entry.isDirectory() &&
                                                entry.getName().endsWith(".pdf") && entry.getSize() > 0
                                                && !entry.getName().startsWith("__MACOSX")) {
                                        System.out.println("File processing ... " + entry.getName());
                                        Date todaysDate = new Date();
                                        String fileName = entry.getName();
                                        Data elem = new Data(fileName, "src_default", todaysDate);
                                        dataStore.add(elem);
                                        try (InputStream inpStreamPdf = zipFile.getInputStream(entry)) {
                                                try (PDDocument pdfDoc = PDDocument.load(inpStreamPdf)) {
                                                        PDFTextStripper extractTxtTool = new PDFTextStripper();
                                                        String textOnPdf = extractTxtTool.getText(pdfDoc);
                                                        filesStr.add(textOnPdf);
                                                } catch (Exception e) {

                                                        System.err.println("file name failed:" + entry.getName()
                                                                        + "Due to " + e.getMessage());
                                                }
                                        } catch (Exception e) {

                                                System.err.println("file name failed: " + entry.getName() + "Due to "
                                                                + e.getMessage());
                                        }

                                }

                        }
                        zipFile.close();
                        String path = System.getenv("ENVIRON_PATH");
                        Dotenv loadEnvVar = Dotenv.configure()
                                        .directory(path)
                                        .filename(".env")
                                        .load();

                        String establishConn = loadEnvVar.get("CONNECTION_URI");

                        MongoClient mongodbClient = MongoClients.create(establishConn);

                        OllamaEmbeddingModel embModel = OllamaEmbeddingModel.builder()
                                        .baseUrl("http://localhost:11434/")
                                        .modelName("nomic-embed-text")
                                        .build();

                        if (!DatabaseReady.existing(mongodbClient, "rag_txt")) {
                                List<Document> documentFiles = new ArrayList<>();

                                for (int i = 0; i < filesStr.size(); i++) {
                                        DateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

                                        Metadata fileInfoMetaData = Metadata.from(Map.of(
                                                        "File Name", dataStore.get(i).fileName(),
                                                        "Source", dataStore.get(i).source(),
                                                        "Date Added", df.format(dataStore.get(i).date())));

                                        String contentOfFile = filesStr.get(i);
                                        Document doc = Document.from(contentOfFile, fileInfoMetaData);
                                        documentFiles.add(doc);
                                }

                                DocumentSplitter noOverlapSplitter = DocumentSplitters.recursive(500, 0);

                                List<TextSegment> textSeg = noOverlapSplitter.splitAll(documentFiles);

                                List<Embedding> embeddings = embModel.embedAll(textSeg).content();
                                Bson filter_none = null;
                                Set<String> metaDataFieldsStr = new HashSet<>();

                                IndexMapping mapIndex = new IndexMapping(768, metaDataFieldsStr);

                                EmbeddingStore<TextSegment> embStore = new MongoDbEmbeddingStore(
                                                mongodbClient,
                                                "rag_txt",
                                                "documents",
                                                "embedding",
                                                10L,
                                                new CreateCollectionOptions(),
                                                filter_none,
                                                mapIndex,
                                                true);

                                embStore.addAll(embeddings, textSeg);
                        }

                        Bson filter_none = null;
                        Set<String> metaDataFieldsStr = new HashSet<>();

                        IndexMapping mapIndex = new IndexMapping(768, metaDataFieldsStr);
                        EmbeddingStore<TextSegment> embStore = new MongoDbEmbeddingStore(
                                        mongodbClient,
                                        "rag_txt",
                                        "documents",
                                        "embedding",
                                        10L,
                                        new CreateCollectionOptions(),
                                        filter_none,
                                        mapIndex,
                                        true);
                        ContentRetriever contentRetrieverFromStore = EmbeddingStoreContentRetriever.builder()
                                        .embeddingStore(embStore)
                                        .embeddingModel(embModel)
                                        .maxResults(6)
                                        .minScore(0.6)
                                        .build();

                        WebSearchEngine tavilySearchEngine = TavilyWebSearchEngine.builder()
                                        .apiKey(System.getenv("API_KEY_SEARCH_ENGINE"))
                                        .build();
                        ContentRetriever contentRetrieverFromWeb = WebSearchContentRetriever.builder()
                                        .webSearchEngine(tavilySearchEngine)
                                        .maxResults(5)
                                        .build();
                        QueryRouter queryRouterWithEmbAndWeb = new DefaultQueryRouter(contentRetrieverFromStore,
                                        contentRetrieverFromWeb);

                        ContentInjector contentInjectorToModel = DefaultContentInjector.builder()
                                        .promptTemplate(
                                                        PromptTemplate.from("{{userMessage}}\n" +
                                                                        "\n" +
                                                                        "Use relevent information from the stored documents:\n"
                                                                        +
                                                                        "{{contents}}. "))
                                        .metadataKeysToInclude(List.of("file_name"))
                                        .build();
                        // use .contentRetriever(contentRetrieverFromStore) without queryRotuer if only
                        // want to search internal documents and no web searches
                        RetrievalAugmentor retrievalAugmentorToModel = DefaultRetrievalAugmentor.builder()
                                        .queryRouter(queryRouterWithEmbAndWeb)
                                        .contentInjector(contentInjectorToModel)
                                        .build();

                        assistant = AiServices.builder(Assistant.class)
                                        .chatModel(model)
                                        .chatMemoryProvider(
                                                        sessionId -> MessageWindowChatMemory
                                                                        .withMaxMessages(MAX_MESSAGES))
                                        .retrievalAugmentor(retrievalAugmentorToModel)
                                        .build();

                }

                return assistant;
        }

        public String chat(String sessionId, String message) throws Exception {
                String reply = getAssistant().chat(sessionId, message).trim();
                int i = reply.lastIndexOf(message);
                return i > 0 ? reply.substring(i) : reply;
        }

}
