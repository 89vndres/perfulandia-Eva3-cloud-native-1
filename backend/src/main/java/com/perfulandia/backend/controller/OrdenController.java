package com.perfulandia.backend.controller;

import com.perfulandia.backend.model.*;
import com.perfulandia.backend.repository.*;
import com.perfulandia.backend.messaging.kafka.dto.FacturacionEvento;
import com.perfulandia.backend.messaging.kafka.dto.OrdenEvento;
import com.perfulandia.backend.messaging.kafka.producer.KafkaEventProducer;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/ordenes")
@CrossOrigin(origins = "${app.cors.allowed-origin:http://localhost:3000}")
public class OrdenController {
    private final OrdenRepository ordenRepository;
    private final PerfumeRepository perfumeRepository;
    private final KafkaEventProducer kafkaProducer;

    public OrdenController(OrdenRepository ordenRepository, PerfumeRepository perfumeRepository,
                           KafkaEventProducer kafkaProducer) {
        this.ordenRepository = ordenRepository;
        this.perfumeRepository = perfumeRepository;
        this.kafkaProducer = kafkaProducer;
    }

    @GetMapping("/mis-compras")
    public List<Orden> misCompras(@RequestParam String email) { return ordenRepository.findByUsuarioEmail(email); }

    @GetMapping("/todas")
    public List<Orden> todasLasOrdenes() { return ordenRepository.findAll(); }

    @PostMapping
    @Transactional
    public ResponseEntity<Orden> crearOrden(@RequestBody Orden orden) {
        if (orden.getDetalles() == null || orden.getDetalles().isEmpty()
                || orden.getUsuarioEmail() == null || orden.getUsuarioEmail().isBlank()
                || orden.getTotal() == null || orden.getTotal() < 0) return ResponseEntity.badRequest().build();
        orden.setFecha(new Date());
        orden.setEstado("Pagado");
        // Validar todos los detalles y stocks antes de modificar inventario.
        for (DetalleOrden detalle : orden.getDetalles()) {
            if (detalle.getPerfumeId() == null || detalle.getCantidad() == null || detalle.getCantidad() <= 0
                    || detalle.getPrecioUnitario() == null || detalle.getPrecioUnitario() < 0)
                return ResponseEntity.badRequest().build();
            Perfume perfume = perfumeRepository.findById(detalle.getPerfumeId()).orElse(null);
            if (perfume == null || perfume.getStock() < detalle.getCantidad()) return ResponseEntity.badRequest().build();
        }
        for (DetalleOrden detalle : orden.getDetalles()) {
            detalle.setOrden(orden);
            Perfume perfume = perfumeRepository.findById(detalle.getPerfumeId()).orElseThrow();
            perfume.setStock(perfume.getStock() - detalle.getCantidad());
            perfumeRepository.save(perfume);
        }
        Orden saved = ordenRepository.save(orden);
        List<OrdenEvento.Detalle> details = saved.getDetalles().stream()
                .map(d -> new OrdenEvento.Detalle(d.getPerfumeId(), d.getCantidad(), d.getPrecioUnitario())).toList();
        OrdenEvento orderEvent = new OrdenEvento(saved.getId(), saved.getUsuarioEmail(), saved.getTotal(),
                saved.getEstado(), saved.getFecha() == null ? Instant.now() : saved.getFecha().toInstant(), details);
        kafkaProducer.publicarOrden(orderEvent).whenComplete((result, error) -> {
            if (error != null) org.slf4j.LoggerFactory.getLogger(OrdenController.class)
                    .error("No se pudo publicar evento de orden id={} en Kafka", saved.getId(), error);
        });
        FacturacionEvento billingEvent = new FacturacionEvento(saved.getId(), saved.getUsuarioEmail(),
                saved.getTotal(), "CLP", "PENDIENTE", Instant.now());
        kafkaProducer.publicarFacturacion(billingEvent).whenComplete((result, error) -> {
            if (error != null) org.slf4j.LoggerFactory.getLogger(OrdenController.class)
                    .error("No se pudo publicar evento de facturación para orden id={}", saved.getId(), error);
        });
        return ResponseEntity.ok(saved);
    }
}
