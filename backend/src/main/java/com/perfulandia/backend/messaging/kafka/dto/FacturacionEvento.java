package com.perfulandia.backend.messaging.kafka.dto;

import java.time.Instant;

public record FacturacionEvento(
        Long ordenId,
        String usuarioEmail,
        Double monto,
        String moneda,
        String estado,
        Instant fecha
) {}
