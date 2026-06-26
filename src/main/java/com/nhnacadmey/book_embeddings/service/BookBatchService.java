package com.nhnacadmey.book_embeddings.service;

import com.nhnacadmey.book_embeddings.dto.BookDto;
import com.nhnacadmey.book_embeddings.dto.ReviewDto;

import java.util.List;

public interface BookBatchService {
    void processAndSaveEmbeddings(List<BookDto> bookDtos);
    void BookReviewAndUpdateEmbeddings(List<ReviewDto> reviewDtos);
}
