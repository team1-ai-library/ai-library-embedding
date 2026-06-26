package com.nhnacadmey.book_embeddings.service.impl;

import com.nhnacadmey.book_embeddings.dto.BookDto;
import com.nhnacadmey.book_embeddings.dto.Reviewdto;
import com.nhnacadmey.book_embeddings.service.BookBatchService;
import com.nhnacadmey.book_embeddings.util.TextPreprocessor;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.util.StringUtils;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Service
public class BookBatchServiceImpl implements BookBatchService {

    private static final Logger log = LoggerFactory.getLogger(BookBatchServiceImpl.class);

    private final EmbeddingModel embeddingModel;
    private final JdbcTemplate jdbcTemplate;

    private static final int CHUNK_SIZE = 16;

    @Override
    @Transactional
    public void processAndSaveEmbeddings(List<BookDto> bookDtos) {
        String sql = "INSERT INTO book_embeddings (book_id, embedding, created_at) VALUES (?, ?, ?)";
        Timestamp currentTimestamp = Timestamp.valueOf(LocalDateTime.now());

        int total = bookDtos.size();
        int processed = 0;

        for (int i = 0; i < total; i += CHUNK_SIZE) {
            List<BookDto> chunk = bookDtos.subList(i, Math.min(i + CHUNK_SIZE, total));

            long embedStart = System.currentTimeMillis();
            List<float[]> vectors = embedAllForBook(chunk);
            long embedMs = System.currentTimeMillis() - embedStart;

            long insertStart = System.currentTimeMillis();
            executeBatchInsert(chunk, vectors, sql, currentTimestamp);
            long insertMs = System.currentTimeMillis() - insertStart;

            processed += chunk.size();
            log.info("[Batch] {}/{} 건 완료 | 임베딩: {}ms, DB insert: {}ms (누적 처리율: {}%)",
                    processed, total, embedMs, insertMs,
                    String.format("%.1f", (double) processed / total * 100));
        }
    }

    // 청크 내 모든 텍스트를 한 번의 batch call로 임베딩
    private List<float[]> embedAllForBook(List<BookDto> chunk) {
        List<String> texts = chunk.stream()
                .map(this::buildEmbedText)
                .toList();
        return embeddingModel.embed(texts);
    }

    private void executeBatchInsert(List<BookDto> chunk, List<float[]> vectors, String sql, Timestamp timestamp) {
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                float[] vector = vectors.get(i);
                Float[] objectVector = new Float[vector.length];
                for (int j = 0; j < vector.length; j++) {
                    objectVector[j] = vector[j];
                }
                java.sql.Array sqlArray = ps.getConnection().createArrayOf("float4", objectVector);

                ps.setLong(1, chunk.get(i).id());
                ps.setArray(2, sqlArray);
                ps.setTimestamp(3, timestamp);
            }

            @Override
            public int getBatchSize() {
                return chunk.size();
            }
        });
    }

    private String buildEmbedText(BookDto dto) {
        StringBuilder sb = new StringBuilder();

        if (StringUtils.hasText(dto.title())) {
            sb.append("[제목] ").append(TextPreprocessor.preprocess(dto.title())).append(" ");
        }
        if (StringUtils.hasText(dto.authorName())) {
            sb.append("[저자] ").append(TextPreprocessor.preprocess(dto.authorName())).append(" ");
        }
        if (StringUtils.hasText(dto.bookContent())) {
            sb.append("[내용] ").append(TextPreprocessor.preprocess(dto.bookContent()));
        }

        return sb.toString().trim();
    }

    @Override
    @Transactional
    public void BookReviewAndUpdateEmbeddings(List<Reviewdto> reviewDtos) {
        String sql = "UPDATE book_embeddings SET embedding = ? WHERE book_id = ?";

        int total = reviewDtos.size();
        int processed = 0;

        for (int i = 0; i < total; i += CHUNK_SIZE) {
            List<Reviewdto> chunk = reviewDtos.subList(i, Math.min(i + CHUNK_SIZE, total));

            long embedStart = System.currentTimeMillis();
            List<float[]> vectors = embedAllForBookReview(chunk);
            long embedMs = System.currentTimeMillis() - embedStart;

            long updateStart = System.currentTimeMillis();
            executeBatchUpdate(chunk, vectors, sql);
            long updateMs = System.currentTimeMillis() - updateStart;

            processed += chunk.size();
            log.info("[Batch] {}/{} 건 완료 | 임베딩: {}ms, DB update: {}ms (누적 처리율: {}%)",
                    processed, total, embedMs, updateMs,
                    String.format("%.1f", (double) processed / total * 100));
        }
    }

    private void executeBatchUpdate(List<Reviewdto> chunk, List<float[]> vectors, String sql) {
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                float[] vector = vectors.get(i);
                Float[] objectVector = new Float[vector.length];
                for (int j = 0; j < vector.length; j++) {
                    objectVector[j] = vector[j];
                }
                java.sql.Array sqlArray = ps.getConnection().createArrayOf("float4", objectVector);

                ps.setArray(1, sqlArray);
                ps.setLong(2, chunk.get(i).bookDto().id());
            }

            @Override
            public int getBatchSize() {
                return chunk.size();
            }
        });
    }

    private List<float[]> embedAllForBookReview(List<Reviewdto> chunk) {
        List<String> texts = chunk.stream()
                .map(this::buildEmbedText)
                .toList();
        return embeddingModel.embed(texts);
    }

    private String buildEmbedText(Reviewdto dto) {
        StringBuilder sb = new StringBuilder();

        if (StringUtils.hasText(dto.bookDto().title())) {
            sb.append("[제목] ").append(TextPreprocessor.preprocess(dto.bookDto().title())).append(" ");
        }
        if (StringUtils.hasText(dto.bookDto().authorName())) {
            sb.append("[저자] ").append(TextPreprocessor.preprocess(dto.bookDto().authorName())).append(" ");
        }
        if (StringUtils.hasText(dto.bookDto().bookContent())) {
            sb.append("[내용] ").append(TextPreprocessor.preprocess(dto.bookDto().bookContent())).append(" ");
        }
        if (StringUtils.hasText(dto.reviewSummary())) {
            sb.append("[리뷰 요약] ").append(TextPreprocessor.preprocess(dto.reviewSummary()));
        }

        return sb.toString().trim();
    }
}
