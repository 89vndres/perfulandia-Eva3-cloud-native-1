package com.perfulandia.backend.messaging.kafka.controller;

import com.perfulandia.backend.messaging.kafka.dto.KafkaTopicConfigRequest;
import com.perfulandia.backend.messaging.kafka.dto.KafkaTopicRequest;
import com.perfulandia.backend.messaging.kafka.service.KafkaAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/kafka")
public class KafkaAdminController {
    private final KafkaAdminService service;

    public KafkaAdminController(KafkaAdminService service) {
        this.service = service;
    }

    @GetMapping("/topics")
    public ResponseEntity<?> listarTopicos() {
        try { return ResponseEntity.ok(service.listarTopicos()); }
        catch (Exception e) { return error(e); }
    }

    @PostMapping("/topics")
    public ResponseEntity<?> crearTopico(@Valid @RequestBody KafkaTopicRequest request) {
        try {
            service.crearTopico(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("mensaje", "Tópico creado", "topic", request.name()));
        } catch (Exception e) { return error(e); }
    }

    @PutMapping("/topics/{name}")
    public ResponseEntity<?> actualizarTopico(@PathVariable String name,
            @Valid @RequestBody KafkaTopicConfigRequest request) {
        try {
            service.actualizarTopico(name, request);
            return ResponseEntity.ok(Map.of("mensaje", "Configuración del tópico actualizada", "topic", name));
        } catch (Exception e) { return error(e); }
    }

    @DeleteMapping("/topics/{name}")
    public ResponseEntity<?> eliminarTopico(@PathVariable String name) {
        try {
            service.eliminarTopico(name);
            return ResponseEntity.ok(Map.of("mensaje", "Tópico eliminado", "topic", name));
        } catch (Exception e) { return error(e); }
    }

    @GetMapping("/consumer-groups")
    public ResponseEntity<?> listarConsumerGroups() {
        try { return ResponseEntity.ok(service.listarConsumerGroups()); }
        catch (Exception e) { return error(e); }
    }

    @GetMapping("/consumer-groups/{groupId}")
    public ResponseEntity<?> detalleConsumerGroup(@PathVariable String groupId) {
        try { return ResponseEntity.ok(service.detalleConsumerGroup(groupId)); }
        catch (Exception e) { return error(e); }
    }

    private ResponseEntity<Map<String, String>> error(Exception e) {
        String detail = e.getCause() != null && e.getCause().getMessage() != null
                ? e.getCause().getMessage() : e.getMessage();
        if (detail == null || detail.isBlank()) detail = "Error sin detalle disponible";
        HttpStatus status = e instanceof IllegalArgumentException ? HttpStatus.BAD_REQUEST : HttpStatus.BAD_GATEWAY;
        return ResponseEntity.status(status).body(Map.of(
                "error", "No se pudo completar la operación Kafka", "detalle", detail));
    }
}
