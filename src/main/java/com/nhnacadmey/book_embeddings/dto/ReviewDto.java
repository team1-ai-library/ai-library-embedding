package com.nhnacadmey.book_embeddings.dto;

public record ReviewDto(
        BookDto bookDto,
        String reviewSummary
) {
}
