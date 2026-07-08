package com.pulse.metrics.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String EVENTS_TOPIC = "pulse-metrics-events";
    public static final String ANOMALIES_TOPIC = "pulse-metrics-anomalies";
    public static final String REPORTS_TOPIC = "pulse-metrics-reports";

    @Bean
    public NewTopic eventsTopic() {
        return TopicBuilder.name(EVENTS_TOPIC)
                .partitions(2)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic anomaliesTopic() {
        return TopicBuilder.name(ANOMALIES_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic reportsTopic() {
        return TopicBuilder.name(REPORTS_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
