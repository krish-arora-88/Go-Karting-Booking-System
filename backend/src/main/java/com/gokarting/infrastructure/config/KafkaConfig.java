package com.gokarting.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@Profile("!no-kafka")
public class KafkaConfig {

    @Value("${app.kafka.topics.booking-events}")
    private String bookingEventsTopic;

    @Value("${app.kafka.topics.notification-requests}")
    private String notificationRequestsTopic;

    // ===== Topic definitions (auto-create with sensible settings) =====

    @Bean
    public NewTopic bookingEventsTopic() {
        return TopicBuilder.name(bookingEventsTopic)
                .partitions(3)          // parallelism for consumers
                .replicas(1)            // single broker for dev; set to 3 in production
                .compact()              // log compaction — keep latest event per key
                .build();
    }

    @Bean
    public NewTopic notificationRequestsTopic() {
        return TopicBuilder.name(notificationRequestsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic bookingEventsDlt() {
        return TopicBuilder.name(bookingEventsTopic + ".DLT")
                .partitions(1)
                .replicas(1)
                .build();
    }

    /**
     * Error handler: retry twice with 1-second pause, then send to DLT.
     * DeadLetterPublishingRecoverer preserves original topic + partition in headers.
     */
    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<String, Object> template) {
        var recoverer = new DeadLetterPublishingRecoverer(template);
        var backOff = new FixedBackOff(1000L, 2L);
        return new DefaultErrorHandler(recoverer, backOff);
    }
}
