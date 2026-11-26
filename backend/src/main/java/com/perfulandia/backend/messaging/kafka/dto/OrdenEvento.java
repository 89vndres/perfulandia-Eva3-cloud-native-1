package com.perfulandia.backend.messaging.kafka.dto;

import java.time.Instant;
import java.util.List;

public record OrdenEvento(
        Long ordenId,
        String usuarioEmail,
        Double total,
        String estado,
        Instant fecha,
        List<Detalle> detalles
) {
    public record Detalle(Long perfumeId, Integer cantidad, Double precioUnitario) {}
}
