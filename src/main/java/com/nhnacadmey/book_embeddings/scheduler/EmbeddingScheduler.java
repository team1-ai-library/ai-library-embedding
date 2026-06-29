package com.nhnacadmey.book_embeddings.scheduler;

import com.nhnacadmey.book_embeddings.dto.BookDto;
import com.nhnacadmey.book_embeddings.entity.Book;
import com.nhnacadmey.book_embeddings.repository.BookRepository;
import com.nhnacadmey.book_embeddings.service.BookBatchService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.util.List;

@Component
@ConditionalOnProperty(name = "embedding.batch.enabled", havingValue = "true")
@RequiredArgsConstructor
public class EmbeddingScheduler {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingScheduler.class);

    private final BookRepository bookRepository;
    private final BookBatchService bookBatchService;

    @Value("${embedding.batch.page-size:50}")
    private int pageSize;

    @Scheduled(
            fixedDelayString = "${embedding.batch.fixed-delay-ms:300000}",
            initialDelayString = "${embedding.batch.initial-delay-ms:5000}"
    )
    public void runBatch() {
        long remaining = bookRepository.countBooksNotYetEmbedded();

        if (remaining == 0) {
            log.info("[Scheduler] 모든 도서 임베딩 완료. 다음 확인까지 대기...");
            return;
        }

        long total = bookRepository.count();
        long alreadyDone = total - remaining;
        log.info("[Scheduler] ===== 배치 시작 =====");
        log.info("[Scheduler] 전체: {}건 | 처리 완료: {}건 | 미처리: {}건 (진행률: {}%)",
                total, alreadyDone, remaining,
                String.format("%.1f", (double) alreadyDone / total * 100));
        log.info("[Scheduler] 이번 배치 대상: 최대 {}건", pageSize);

        StopWatch sw = new StopWatch();
        sw.start();

        try {
            Page<Book> page = bookRepository.findBooksNotYetEmbedded(PageRequest.of(0, pageSize));
            List<BookDto> dtos = page.getContent().stream()
                    .map(this::toDto)
                    .toList();

            bookBatchService.processAndSaveEmbeddings(dtos);

            sw.stop();
            long newRemaining = bookRepository.countBooksNotYetEmbedded();
            log.info("[Scheduler] ===== 배치 완료 =====");
            log.info("[Scheduler] 처리 건수: {}건 | 소요 시간: {}s | 잔여: {}건",
                    dtos.size(), String.format("%.2f", sw.getTotalTimeSeconds()), newRemaining);

            if (newRemaining == 0) {
                log.info("[Scheduler] 전체 도서 임베딩 작업이 완료되었습니다.");
            }
        } catch (Exception e) {
            sw.stop();
            log.error("[Scheduler] 배치 처리 중 오류 발생 (소요: {}s): {}",
                    String.format("%.2f", sw.getTotalTimeSeconds()), e.getMessage(), e);
        }
    }

    private BookDto toDto(Book book) {
        return new BookDto(
                book.getId(),
                book.getIsbn(),
                book.getTitle(),
                book.getAuthorName(),
                book.getBookContent()
        );
    }
}