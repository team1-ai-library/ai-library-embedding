package com.nhnacadmey.book_embeddings.service;

import com.nhnacadmey.book_embeddings.dto.BookDto;
import com.nhnacadmey.book_embeddings.dto.Reviewdto;

import java.util.List;

public interface BookBatchService {
    void processAndSaveEmbeddings(List<BookDto> bookDtos);
    void BookReviewAndUpdateEmbeddings(List<Reviewdto> reviewDtos);
}
