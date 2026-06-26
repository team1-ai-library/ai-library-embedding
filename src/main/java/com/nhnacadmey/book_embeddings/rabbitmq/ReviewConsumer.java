package com.nhnacadmey.book_embeddings.rabbitmq;

import com.nhnacadmey.book_embeddings.dto.Reviewdto;
import com.nhnacadmey.book_embeddings.service.BookBatchService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class ReviewConsumer {
    private static final Logger log = LoggerFactory.getLogger(ReviewConsumer.class);

    private final BookBatchService bookBatchService;

    @RabbitListener(queues = "review.embedding")
    public void processPayment(
            Reviewdto reviewdto,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) throws Exception {

        try {
            processPaymentLogic(reviewdto);

            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.info("[Consumer] 리뷰 임베딩 실패");
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private void processPaymentLogic(Reviewdto reviewdto) {
        bookBatchService.BookReviewAndUpdateEmbeddings(List.of(reviewdto));
    }
}