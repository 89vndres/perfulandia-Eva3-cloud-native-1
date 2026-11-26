package com.perfulandia.backend.messaging.kafka.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record KafkaTopicConfigRequest(
        @Min(1) long retentionMs,
        @NotBlank @Pattern(regexp = "delete|compact|compact,delete") String cleanupPolicy,
        @Min(1) Integer partitions
) {}
