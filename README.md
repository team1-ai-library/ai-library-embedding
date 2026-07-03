# ai-library-embedding

도서관 시스템에서 도서 정보에 대한 벡터 임베딩을 생성하고 관리하는 Spring Boot 기반 마이크로서비스입니다.

## 개요

이 서비스는 두 가지 방식으로 도서 임베딩을 처리합니다.

1. **배치 스케줄러**: 임베딩이 아직 생성되지 않은 도서를 주기적으로 탐색해 자동 처리
2. **RabbitMQ 컨슈머**: 리뷰 이벤트를 수신해 해당 도서의 임베딩을 리뷰 요약 정보 포함 내용으로 갱신

임베딩 모델로 **BGE-M3** (1024차원)를 사용하며, 생성된 벡터는 PostgreSQL의 pgvector 확장을 통해 저장됩니다.

## 기술 스택

| 항목 | 내용 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 4.1.0 |
| AI | Spring AI 2.0.0, OpenAI-호환 API (BGE-M3 모델) |
| Database | PostgreSQL + pgvector |
| Message Queue | RabbitMQ (AMQP) |
| Build | Maven |

## 아키텍처

```
[도서 DB (books)]
       │
       ▼
[EmbeddingScheduler]  ──────────────────────────────►  [BookBatchServiceImpl]
  (미임베딩 도서 조회)                                        │
                                                        ├─ TextPreprocessor (전처리)
[RabbitMQ]                                              ├─ EmbeddingModel (BGE-M3 호출)
  library.team1.review.exchange                         └─ JdbcTemplate (배치 INSERT/UPDATE)
       │                                                        │
       ▼                                                        ▼
[ReviewConsumer]  ─────────────────────────────────►  [book_embeddings 테이블]
  (리뷰 이벤트 수신)                                      (book_id, embedding, created_at)
       │
       └─ 실패 시 → DLX → DLQ (library.team1.review.embedding.dlq)
```

## 주요 컴포넌트

### 서비스 계층

- **`BookBatchServiceImpl`**: 핵심 서비스. 도서를 16건 단위(CHUNK_SIZE)로 묶어 임베딩 API를 배치 호출하고, JdbcTemplate으로 DB에 삽입/갱신
- **`TextPreprocessor`**: HTML 엔티티 디코딩 → HTML 태그 제거 → 특수문자 제거 → 소문자 변환 순서로 텍스트 전처리

### 스케줄러

- **`EmbeddingScheduler`**: `@Scheduled` 기반. `embedding.batch.enabled=true`일 때만 활성화되며, 미처리 도서를 페이지 단위로 조회해 배치 처리

### RabbitMQ

- **`ReviewConsumer`**: `library.team1.review.embedding` 큐 구독. 리뷰 DTO 수신 후 `BookReviewAndUpdateEmbeddings` 호출. 실패 시 nack → DLQ 이동

### 임베딩 텍스트 구성 형식

```
[제목] {title} [저자] {authorName} [내용] {bookContent}
```

리뷰가 있는 경우:
```
[제목] {title} [저자] {authorName} [내용] {bookContent} [리뷰 요약] {reviewSummary}
```

## RabbitMQ 토폴로지

| 리소스 | 이름 |
|--------|------|
| Exchange (Topic) | `library.team1.review.exchange` |
| Queue | `library.team1.review.embedding` |
| 라우팅 키 | `library.team1.review.#` |
| DLX (Direct) | `library.team1.review.dlx` |
| DLQ | `library.team1.review.embedding.dlq` |

## 설정 (application.yaml)

### 필수 환경 변수

| 환경 변수 | 설명 |
|-----------|------|
| `postgresql-url` | PostgreSQL JDBC URL |
| `postgresql-username` | DB 사용자명 |
| `postgresql-password` | DB 비밀번호 |
| `openai-url` | OpenAI 호환 API Base URL |
| `openai-api-key` | API 키 |
| `rabbitmq-host` | RabbitMQ 호스트 |
| `rabbitmq-port` | RabbitMQ 포트 (기본 5672) |
| `rabbitmq-username` | RabbitMQ 사용자명 |
| `rabbitmq-password` | RabbitMQ 비밀번호 |

> `.env` 파일을 통해 환경 변수를 주입하거나, 실행 시 `-D` 옵션으로 전달하세요.  
> `.env` 파일은 `.gitignore`에 포함되어 있어 커밋되지 않습니다.

### 배치 설정

```yaml
embedding:
  batch:
    enabled: true           # false 시 스케줄러 비활성화
    page-size: 500          # 배치 1회 처리 건수
    chunk-size: 16          # 임베딩 API 1회 호출당 텍스트 수
    fixed-delay-ms: 180000  # 배치 간격 (ms), 기본 3분
    initial-delay-ms: 5000  # 앱 시작 후 첫 실행 대기 시간 (ms)
```

### pgvector 설정

```yaml
spring:
  ai:
    vectorstore:
      pgvector:
        table-name: vector_store
        index-type: HNSW
        distance-type: COSINE_DISTANCE
        dimensions: 1024    # BGE-M3 모델 차원 수
        initialize-schema: true
```

## 서버 포트

```
8082
```

## 빌드 및 실행

```bash
# 빌드
./mvnw clean package -DskipTests

# 실행
./mvnw spring-boot:run
```

## 테스트

```bash
# 단위/통합 테스트 실행 (실제 DB 및 임베딩 서버 필요)
./mvnw test
```

### 테스트 클래스 설명

- **`BookBatchServiceTest`**: CSV 샘플 데이터(`BOOK_DB_202112_filled.csv`)를 활용한 통합 테스트
  - `testProcessAndSaveEmbeddings`: 10건 도서에 대해 임베딩 생성 및 저장 검증
  - `testBookReviewAndUpdateEmbeddings`: 리뷰 임베딩 갱신 검증
  - `performanceTestProcessAndSaveAllEmbeddings`: 전체 데이터 성능 테스트 (`@Disabled` — 수동 실행)

## 데이터베이스 스키마

### books 테이블 (읽기 전용)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT | PK |
| isbn | VARCHAR(20) | ISBN |
| title | VARCHAR(500) | 도서 제목 |
| author_name | VARCHAR(1000) | 저자명 |
| publisher_name | VARCHAR(255) | 출판사명 |
| book_content | TEXT | 도서 소개 |
| first_publish_date | DATE | 초판 발행일 |
| price | DECIMAL(10,2) | 가격 |

### book_embeddings 테이블 (이 서비스가 관리)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT | PK (AUTO) |
| book_id | BIGINT | books.id 참조 |
| embedding | float4[] | 1024차원 벡터 |
| created_at | TIMESTAMP | 생성 시각 |