package com.nhnacadmey.book_embeddings.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public TopicExchange reviewExchange() {
        return new TopicExchange("review.exchange");
    }

    @Bean
    public Queue reviewQueue() {
        return QueueBuilder.durable("review.embedding")
            .withArgument("x-dead-letter-exchange", "review.dlx")
            .build();
    }

    @Bean
    public Binding reviewBinding() {
        return BindingBuilder
            .bind(reviewQueue())
            .to(reviewExchange())
            .with("review.#");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}