package com.perfulandia.backend.messaging.kafka.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record KafkaTopicRequest(
        @NotBlank @Pattern(regexp = "[a-zA-Z0-9._-]{1,249}") String name,
        @Min(1) int partitions,
        @Min(1) short replicas,
        @Min(1) long retentionMs,
        @NotBlank @Pattern(regexp = "delete|compact|compact,delete") String cleanupPolicy
) {}
