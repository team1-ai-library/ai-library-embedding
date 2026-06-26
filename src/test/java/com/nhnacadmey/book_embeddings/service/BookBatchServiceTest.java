package com.nhnacadmey.book_embeddings.service;

import com.nhnacadmey.book_embeddings.dto.BookDto;
import com.nhnacadmey.book_embeddings.dto.Reviewdto;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class BookBatchServiceTest {

    private static final Logger log = LoggerFactory.getLogger(BookBatchServiceTest.class);

    @Autowired
    private BookBatchService bookBatchService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testProcessAndSaveEmbeddings() throws Exception {
        // Given: CSV 파일에서 데이터를 파싱하여 BookDto 리스트 생성 (테스트용 10개만)
        List<BookDto> bookDtos = readBooksFromCsv(10);

        assertThat(bookDtos).isNotEmpty();

        List<Long> bookIds = bookDtos.stream().map(BookDto::id).toList();

        // 테스트 데이터 격리를 위해 기존에 테스트 대상 도서들의 임베딩만 삭제 (전체 삭제 X)
        if (!bookIds.isEmpty()) {
            jdbcTemplate.update("DELETE FROM book_embeddings WHERE book_id IN (" +
                    String.join(",", bookIds.stream().map(Object::toString).toList()) + ")");
        }

        // When: 임베딩 처리 및 저장
        bookBatchService.processAndSaveEmbeddings(bookDtos);

        // Then: DB에 정상 저장되었는지 개별 검증
        for (Long bookId : bookIds) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM book_embeddings WHERE book_id = ?", Integer.class, bookId);
            assertThat(count).isGreaterThanOrEqualTo(1);
        }
    }

    @Test
    void testBookReviewAndUpdateEmbeddings() throws Exception {
        // Given: 책 2개를 DB에 저장
        List<BookDto> bookDtos = readBooksFromCsv(2);
        assertThat(bookDtos).hasSize(2);

        List<Long> bookIds = bookDtos.stream().map(BookDto::id).toList();

        // 테스트 데이터 격리를 위해 기존에 테스트 대상 도서들의 임베딩만 삭제 (전체 삭제 X)
        if (!bookIds.isEmpty()) {
            jdbcTemplate.update("DELETE FROM book_embeddings WHERE book_id IN (" +
                    String.join(",", bookIds.stream().map(Object::toString).toList()) + ")");
        }
        
        bookBatchService.processAndSaveEmbeddings(bookDtos);

        // When: 이 책들에 대해 리뷰를 생성하고 임베딩을 업데이트
        List<Reviewdto> reviewDtos = new ArrayList<>();
        reviewDtos.add(new Reviewdto(bookDtos.get(0), "이 책은 정말 재미있습니다. 추천합니다."));
        reviewDtos.add(new Reviewdto(bookDtos.get(1), "내용이 너무 어려워서 이해하기 힘드네요."));

        bookBatchService.BookReviewAndUpdateEmbeddings(reviewDtos);

        // Then: DB에 저장된 임베딩 개별 검증
        for (Long bookId : bookIds) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM book_embeddings WHERE book_id = ?", Integer.class, bookId);
            assertThat(count).isGreaterThanOrEqualTo(1);
        }
    }

    @Test
    @Disabled("전체 15만 건의 데이터를 파싱하고 임베딩하여 삽입하는 성능 테스트입니다. 수동 실행 시 이 어노테이션을 제거하세요.")
    void performanceTestProcessAndSaveAllEmbeddings() throws Exception {
        // Given: CSV 파일 전체를 파싱
        StopWatch stopWatch = new StopWatch("Book Embeddings Performance Test");
        
        stopWatch.start("CSV Parsing");
        List<BookDto> allBookDtos = readBooksFromCsv(Integer.MAX_VALUE);
        stopWatch.stop();
        
        log.info("CSV 파싱 완료. 총 {}개의 책 데이터를 읽었습니다. 소요 시간: {}s", 
                allBookDtos.size(), stopWatch.getLastTaskTimeMillis() / 1000.0);

        assertThat(allBookDtos).isNotEmpty();

        // 데이터 정리를 위해 기존 테이블 비우기 제거 (성능 테스트 시 수동 실행 전용)
        // jdbcTemplate.execute("TRUNCATE TABLE book_embeddings");

        // When: 전체 임베딩 및 저장 (성능 측정 시작)
        stopWatch.start("Embedding & Batch Insert");
        bookBatchService.processAndSaveEmbeddings(allBookDtos);
        stopWatch.stop();

        // Then: DB 저장 데이터 확인
        Integer dbCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM book_embeddings", Integer.class);
        log.info("성능 테스트 결과: {}개 데이터 저장 완료.", dbCount);
        log.info("전체 소요 시간 및 상세 정보:\n{}", stopWatch.prettyPrint());
        
        assertThat(dbCount).isEqualTo(allBookDtos.size());
    }

    // CSV 파일로부터 최대 limit 개수만큼 BookDto를 읽어오는 메소드
    private List<BookDto> readBooksFromCsv(int limit) throws Exception {
        ClassPathResource resource = new ClassPathResource("data/BOOK_DB_202112_filled.csv");
        List<BookDto> bookDtos = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String header = br.readLine(); // 헤더 라인 스킵
            
            String line;
            int count = 0;
            while ((line = br.readLine()) != null && count < limit) {
                List<String> tokens = parseCsvLine(line);
                if (tokens.size() < 11) {
                    continue;
                }

                try {
                    long id = Long.parseLong(tokens.get(0));
                    String isbn = tokens.get(1);
                    String title = tokens.get(3);
                    String authorName = tokens.get(4);
                    String bookContent = tokens.get(10);
                    String description = tokens.get(10);

                    BookDto dto = new BookDto(id, isbn, title, authorName, bookContent, description);
                    bookDtos.add(dto);
                    count++;
                } catch (NumberFormatException e) {
                    // ID가 올바른 숫자가 아니면 스킵
                }
            }
        }
        return bookDtos;
    }

    // 따옴표 내부의 콤마를 무시하는 간소화된 CSV 파서
    private List<String> parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes; // 따옴표 진입/퇴출 처리
            } else if (c == ',' && !inQuotes) {
                tokens.add(cleanToken(sb.toString()));
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(cleanToken(sb.toString()));
        return tokens;
    }

    private String cleanToken(String token) {
        String trimmed = token.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed.replace("\"\"", "\"");
    }
}
