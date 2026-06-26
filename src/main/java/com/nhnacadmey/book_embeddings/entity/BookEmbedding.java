package com.nhnacadmey.book_embeddings.entity;

import jakarta.persistence.*;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@Entity
@Table(name = "book_embeddings")
public class BookEmbedding {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "book_id")
    private long bookId;

    @Column
    private List<Float> embedding;

    @Column
    private LocalDateTime created_at;

    public BookEmbedding(long bookId, List<Float> embedding) {
        this.bookId = bookId;
        this.embedding = embedding;

        this.created_at = LocalDateTime.now();
    }
}
