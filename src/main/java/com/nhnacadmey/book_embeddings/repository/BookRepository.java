package com.nhnacadmey.book_embeddings.repository;

import com.nhnacadmey.book_embeddings.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BookRepository extends JpaRepository<Book, Long> {

    @Query(value = """
            SELECT b.* FROM books b
            WHERE NOT EXISTS (SELECT 1 FROM book_embeddings be WHERE be.book_id = b.id)
            ORDER BY b.id
            """,
            countQuery = """
            SELECT COUNT(*) FROM books b
            WHERE NOT EXISTS (SELECT 1 FROM book_embeddings be WHERE be.book_id = b.id)
            """,
            nativeQuery = true)
    Page<Book> findBooksNotYetEmbedded(Pageable pageable);

    @Query(value = """
            SELECT COUNT(*) FROM books b
            WHERE NOT EXISTS (SELECT 1 FROM book_embeddings be WHERE be.book_id = b.id)
            """, nativeQuery = true)
    long countBooksNotYetEmbedded();
}