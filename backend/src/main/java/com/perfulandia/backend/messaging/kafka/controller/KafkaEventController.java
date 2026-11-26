package com.perfulandia.backend.messaging.kafka.controller;

import com.perfulandia.backend.messaging.kafka.dto.FacturacionEvento;
import com.perfulandia.backend.messaging.kafka.dto.OrdenEvento;
import com.perfulandia.backend.messaging.kafka.producer.KafkaEventProducer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/kafka/events")
public class KafkaEventController {
    private final KafkaEventProducer producer;
    public KafkaEventController(KafkaEventProducer producer) { this.producer = producer; }

    @PostMapping("/orders")
    public CompletableFuture<ResponseEntity<?>> publicarOrden(@Valid @RequestBody OrderEventRequest request) {
        OrdenEvento event = new OrdenEvento(request.orderId(), request.email(), request.total(),
                "RECIBIDA", Instant.now(), List.of());
        return producer.publicarOrden(event).<ResponseEntity<?>>thenApply(result ->
                ResponseEntity.accepted().body(Map.of("mensaje", "Evento de orden publicado en Kafka",
                        "topic", result.getRecordMetadata().topic(), "partition", result.getRecordMetadata().partition(),
                        "offset", result.getRecordMetadata().offset())))
                .exceptionally(error -> ResponseEntity.internalServerError().body(Map.of("error", "No se pudo publicar el evento de orden")));
    }

    @PostMapping("/billing")
    public CompletableFuture<ResponseEntity<?>> publicarFacturacion(@Valid @RequestBody BillingEventRequest request) {
        FacturacionEvento event = new FacturacionEvento(request.orderId(), request.email(), request.amount(),
                request.currency() == null || request.currency().isBlank() ? "CLP" : request.currency(), "PENDIENTE", Instant.now());
        return producer.publicarFacturacion(event).<ResponseEntity<?>>thenApply(result ->
                ResponseEntity.accepted().body(Map.of("mensaje", "Evento de facturación publicado en Kafka",
                        "topic", result.getRecordMetadata().topic(), "partition", result.getRecordMetadata().partition(),
                        "offset", result.getRecordMetadata().offset())))
                .exceptionally(error -> ResponseEntity.internalServerError().body(Map.of("error", "No se pudo publicar el evento de facturación")));
    }

    public record OrderEventRequest(@NotNull Long orderId, @NotBlank @Email String email,
                                    @NotNull @PositiveOrZero Double total) {}
    public record BillingEventRequest(@NotNull Long orderId, @NotBlank @Email String email,
                                      @NotNull @PositiveOrZero Double amount, String currency) {}
}
