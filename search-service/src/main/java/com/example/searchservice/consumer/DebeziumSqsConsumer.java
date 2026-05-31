package com.example.searchservice.consumer;

import com.example.searchservice.service.DebeziumOpenSearchIngestionService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DebeziumSqsConsumer {

    private final DebeziumOpenSearchIngestionService ingestionService;

    @SqsListener(
            value = "${app.sqs.debezium-queue-name}",
            maxConcurrentMessages = "${app.sqs.listener.max-concurrent-messages}",
            maxMessagesPerPoll = "${app.sqs.listener.max-messages-per-poll}",
            pollTimeoutSeconds = "${app.sqs.listener.poll-timeout-seconds}",
            messageVisibilitySeconds = "${app.sqs.listener.message-visibility-seconds}"
    )
    public void consume(List<String> messages) {
        ingestionService.process(messages);
    }
}
