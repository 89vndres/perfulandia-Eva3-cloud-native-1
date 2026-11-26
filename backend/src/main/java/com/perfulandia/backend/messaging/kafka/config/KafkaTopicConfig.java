package com.perfulandia.backend.messaging.kafka.config;

import org.apache.kafka.common.config.TopicConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.apache.kafka.clients.admin.NewTopic;

@Configuration
public class KafkaTopicConfig {
    @Value("${app.kafka.topics.ordenes}") private String topicOrdenes;
    @Value("${app.kafka.topics.facturacion}") private String topicFacturacion;
    @Value("${app.kafka.topics.partitions:3}") private int partitions;
    @Value("${app.kafka.topics.replicas:2}") private int replicas;
    @Value("${app.kafka.topics.retention-ms:604800000}") private String retentionMs;

    @Bean
    public NewTopic ordenesTopic() {
        return TopicBuilder.name(topicOrdenes).partitions(partitions).replicas(replicas)
                .config(TopicConfig.RETENTION_MS_CONFIG, retentionMs)
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE).build();
    }

    @Bean
    public NewTopic facturacionTopic() {
        return TopicBuilder.name(topicFacturacion).partitions(partitions).replicas(replicas)
                .config(TopicConfig.RETENTION_MS_CONFIG, retentionMs)
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE).build();
    }
}
