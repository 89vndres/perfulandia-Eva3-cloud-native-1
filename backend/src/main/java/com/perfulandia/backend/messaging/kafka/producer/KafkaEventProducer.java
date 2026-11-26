package com.perfulandia.backend.messaging.kafka.producer;

import com.perfulandia.backend.messaging.kafka.dto.FacturacionEvento;
import com.perfulandia.backend.messaging.kafka.dto.OrdenEvento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class KafkaEventProducer {
    private static final Logger log = LoggerFactory.getLogger(KafkaEventProducer.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String ordenesTopic;
    private final String facturacionTopic;

    public KafkaEventProducer(KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.kafka.topics.ordenes}") String ordenesTopic,
            @Value("${app.kafka.topics.facturacion}") String facturacionTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.ordenesTopic = ordenesTopic;
        this.facturacionTopic = facturacionTopic;
    }

    public CompletableFuture<SendResult<String, Object>> publicarOrden(OrdenEvento evento) {
        return publicar(ordenesTopic, String.valueOf(evento.ordenId()), evento);
    }

    public CompletableFuture<SendResult<String, Object>> publicarFacturacion(FacturacionEvento evento) {
        return publicar(facturacionTopic, String.valueOf(evento.ordenId()), evento);
    }

    private CompletableFuture<SendResult<String, Object>> publicar(String topic, String key, Object payload) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, payload);
        future.whenComplete((result, error) -> {
            if (error != null) log.error("No se pudo publicar evento en {} con key {}", topic, key, error);
            else log.info("Evento publicado en {} partición {} offset {}",
                    topic, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
        });
        return future;
    }
}
