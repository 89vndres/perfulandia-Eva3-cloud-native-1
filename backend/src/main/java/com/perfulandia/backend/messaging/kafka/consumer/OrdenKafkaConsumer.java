
package com.perfulandia.backend.messaging.kafka.consumer;

import com.perfulandia.backend.messaging.kafka.dto.OrdenEvento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OrdenKafkaConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(OrdenKafkaConsumer.class);

    @KafkaListener(
            topics = "${app.kafka.topics.ordenes}",
            groupId = "perfulandia-ordenes-consumer",
            containerFactory = "ordenKafkaListenerContainerFactory"
    )
    public void consumir(OrdenEvento evento) {

        if (evento == null || evento.ordenId() == null || evento.ordenId() <= 0
                || !StringUtils.hasText(evento.usuarioEmail())
                || evento.total() == null || evento.total() < 0
                || !StringUtils.hasText(evento.estado())
                || evento.fecha() == null
                || evento.detalles() == null) {

            throw new IllegalArgumentException(
                    "Evento de orden inválido: faltan datos obligatorios o existen valores fuera de rango"
            );
        }

        boolean invalidDetail = evento.detalles().stream().anyMatch(detail ->
                detail == null
                        || detail.perfumeId() == null
                        || detail.cantidad() == null
                        || detail.cantidad() <= 0
                        || detail.precioUnitario() == null
                        || detail.precioUnitario() < 0
        );

        if (invalidDetail) {
            throw new IllegalArgumentException(
                    "Evento de orden contiene detalles inválidos"
            );
        }

        log.info(
                "Evento Kafka de orden procesado: flujo=ordenes, ordenId={}, email={}, total={}, detalles={}",
                evento.ordenId(),
                evento.usuarioEmail(),
                evento.total(),
                evento.detalles().size()
        );
    }
}
