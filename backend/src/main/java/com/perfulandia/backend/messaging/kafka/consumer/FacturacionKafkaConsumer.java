
package com.perfulandia.backend.messaging.kafka.consumer;

import com.perfulandia.backend.messaging.kafka.dto.FacturacionEvento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class FacturacionKafkaConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(FacturacionKafkaConsumer.class);

    @KafkaListener(
            topics = "${app.kafka.topics.facturacion}",
            groupId = "perfulandia-facturacion-consumer",
            containerFactory = "facturacionKafkaListenerContainerFactory"
    )
    public void consumir(FacturacionEvento evento) {

        if (evento == null || evento.ordenId() == null || evento.ordenId() <= 0
                || !StringUtils.hasText(evento.usuarioEmail())
                || evento.monto() == null || evento.monto() < 0
                || !StringUtils.hasText(evento.moneda())
                || !StringUtils.hasText(evento.estado())
                || evento.fecha() == null) {

            throw new IllegalArgumentException(
                    "Evento de facturación inválido: faltan datos obligatorios o existen valores fuera de rango"
            );
        }

        log.info(
                "Evento Kafka de facturación procesado: ordenId={}, usuario={}, monto={} {}, estado={}",
                evento.ordenId(),
                evento.usuarioEmail(),
                evento.monto(),
                evento.moneda(),
                evento.estado()
        );
    }
}
