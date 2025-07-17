package io.openliberty.sample.langchain4j;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

// tag::Embedding[]
public class Embedding {

    @Pattern(regexp = "^\\d+$", message = "ID Number must be a non-negative integer!")
    private String embeddingID;

    @Pattern(regexp = "(OpenLiberty|JakartaEE|Microprofile)", message = "Content of the embedding must be one of these types!")
    private String tags;

    @NotEmpty(message = "All embeddings must have some content!")
    private String content;

    @NotEmpty(message = "Embeddings can not be empty")
    private float[] embedding;

    public String getEmbeddingID() {
        return embeddingID;
    }

    public void setEmbeddingID(String embeddingID) {
        this.embeddingID = embeddingID;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public float[] getEmbedding() {
        return embedding;
    }

    public void setEmbedding(float[] embedding) {
        this.embedding = embedding;
    }

}
// end::Embedding[]