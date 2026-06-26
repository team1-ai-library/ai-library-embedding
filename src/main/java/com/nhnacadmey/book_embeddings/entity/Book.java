package com.nhnacadmey.book_embeddings.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "books")
@Getter
@NoArgsConstructor
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "book_seq_gen")
    @SequenceGenerator(name = "book_seq_gen", sequenceName = "public.books_seq", allocationSize = 3000)
    @Column(name = "id")
    private Long id;

    @Column(name = "isbn", length = 20)
    private String isbn;

    @Column(name = "volume_title", length = 255)
    private String volumeTitle;

    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "author_name", length = 1000)
    private String authorName;

    @Column(name = "publisher_name", length = 255)
    private String publisherName;

    @Column(name = "first_publish_date")
    private LocalDate firstPublishDate;

    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "book_content", columnDefinition = "TEXT")
    private String bookContent;

    @Column(name = "subtitle", length = 500)
    private String subtitle;

    @Column(name = "edition_publish_date")
    private LocalDate editionPublishDate;

    @Column(name = "created_at", updatable = false, nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public Book(
            String isbn, String volumeTitle, String title, String authorName,
            String publisherName, LocalDate firstPublishDate, BigDecimal price,
            String imageUrl, String bookContent, String subtitle, LocalDate editionPublishDate
    ) {
        this.isbn = isbn;
        this.volumeTitle = volumeTitle;
        this.title = title;
        this.authorName = authorName;
        this.publisherName = publisherName;
        this.firstPublishDate = firstPublishDate;
        this.price = price;
        this.imageUrl = imageUrl;
        this.bookContent = bookContent;
        this.subtitle = subtitle;
        this.editionPublishDate = editionPublishDate;
    }
}