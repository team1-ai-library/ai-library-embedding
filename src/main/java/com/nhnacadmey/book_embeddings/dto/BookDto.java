package com.nhnacadmey.book_embeddings.dto;

public record BookDto(
        long id,
        String isbn,
        String title,
        String authorName,
        String bookContent
) {
}
