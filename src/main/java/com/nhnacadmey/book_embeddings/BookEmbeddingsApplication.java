package com.nhnacadmey.book_embeddings;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BookEmbeddingsApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookEmbeddingsApplication.class, args);
    }

}
