package com.nhnacadmey.book_embeddings.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "embedding.batch")
public class EmbeddingBatchProperties {
    private boolean enabled = true;
    private int pageSize = 50;
    private int chunkSize = 16;
    private long fixedDelayMs = 300000;
    private long initialDelayMs = 5000;
}