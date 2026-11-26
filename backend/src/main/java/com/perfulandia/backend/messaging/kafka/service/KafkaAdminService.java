package com.perfulandia.backend.messaging.kafka.service;

import com.perfulandia.backend.messaging.kafka.dto.KafkaTopicConfigRequest;
import com.perfulandia.backend.messaging.kafka.dto.KafkaTopicRequest;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.config.ConfigResource;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class KafkaAdminService {
    private final Map<String, Object> adminProperties;

    public KafkaAdminService(KafkaAdmin kafkaAdmin) {
        this.adminProperties = kafkaAdmin.getConfigurationProperties();
    }

    private AdminClient client() {
        return AdminClient.create(adminProperties);
    }

    public List<Map<String, Object>> listarTopicos() throws Exception {
        try (AdminClient admin = client()) {
            Set<String> names = admin.listTopics().names().get(15, TimeUnit.SECONDS);
            if (names.isEmpty()) return List.of();
            Map<String, TopicDescription> descriptions =
                    admin.describeTopics(names).allTopicNames().get(15, TimeUnit.SECONDS);
            List<ConfigResource> resources = names.stream()
                    .map(n -> new ConfigResource(ConfigResource.Type.TOPIC, n)).toList();
            Map<ConfigResource, Config> configs =
                    admin.describeConfigs(resources).all().get(15, TimeUnit.SECONDS);

            return names.stream().sorted().map(name -> {
                TopicDescription d = descriptions.get(name);
                Config c = configs.get(new ConfigResource(ConfigResource.Type.TOPIC, name));
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("name", name);
                row.put("partitions", d == null ? 0 : d.partitions().size());
                row.put("replicas", d == null || d.partitions().isEmpty()
                        ? 0 : d.partitions().get(0).replicas().size());
                row.put("retentionMs", configValue(c, "retention.ms"));
                row.put("cleanupPolicy", configValue(c, "cleanup.policy"));
                return row;
            }).toList();
        }
    }

    private String configValue(Config config, String key) {
        if (config == null || config.get(key) == null) return null;
        return config.get(key).value();
    }

    public void crearTopico(KafkaTopicRequest request) throws Exception {
        if (request.partitions() > 100) throw new IllegalArgumentException("No se permiten más de 100 particiones por tópico desde esta API");
        if (request.replicas() > 2) throw new IllegalArgumentException("El clúster local dispone de 2 brokers; el factor de réplica máximo es 2");
        try (AdminClient admin = client()) {
            NewTopic topic = new NewTopic(request.name(), request.partitions(), request.replicas());
            topic.configs(Map.of(
                    "retention.ms", String.valueOf(request.retentionMs()),
                    "cleanup.policy", request.cleanupPolicy()));
            admin.createTopics(List.of(topic)).all().get(20, TimeUnit.SECONDS);
        }
    }

    public void actualizarTopico(String name, KafkaTopicConfigRequest request) throws Exception {
        try (AdminClient admin = client()) {
            int current = admin.describeTopics(List.of(name)).allTopicNames().get(15, TimeUnit.SECONDS)
                    .get(name).partitions().size();
            if (request.partitions() != null && request.partitions() < current) {
                throw new IllegalArgumentException("Kafka no permite reducir particiones de un tópico");
            }
            if (request.partitions() != null && request.partitions() > 100) {
                throw new IllegalArgumentException("No se permiten más de 100 particiones por tópico desde esta API");
            }
            ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, name);
            Map<ConfigResource, Collection<AlterConfigOp>> changes = Map.of(resource, List.of(
                    new AlterConfigOp(new ConfigEntry("retention.ms", String.valueOf(request.retentionMs())), AlterConfigOp.OpType.SET),
                    new AlterConfigOp(new ConfigEntry("cleanup.policy", request.cleanupPolicy()), AlterConfigOp.OpType.SET)));
            admin.incrementalAlterConfigs(changes).all().get(20, TimeUnit.SECONDS);
            if (request.partitions() != null && request.partitions() > current) {
                admin.createPartitions(Map.of(name, NewPartitions.increaseTo(request.partitions())))
                        .all().get(20, TimeUnit.SECONDS);
            }
        }
    }

    public void eliminarTopico(String name) throws Exception {
        try (AdminClient admin = client()) {
            admin.deleteTopics(List.of(name)).all().get(20, TimeUnit.SECONDS);
        }
    }

    public List<Map<String, Object>> listarConsumerGroups() throws Exception {
        try (AdminClient admin = client()) {
            Collection<ConsumerGroupListing> groups =
                    admin.listConsumerGroups().all().get(15, TimeUnit.SECONDS);
            return groups.stream().map(g -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("groupId", g.groupId());
                row.put("isSimpleConsumerGroup", g.isSimpleConsumerGroup());
                row.put("state", g.state().map(Enum::name).orElse("UNKNOWN"));
                return row;
            }).toList();
        }
    }

    public Map<String, Object> detalleConsumerGroup(String groupId) throws Exception {
        try (AdminClient admin = client()) {
            ConsumerGroupDescription group = admin.describeConsumerGroups(List.of(groupId))
                    .describedGroups().get(groupId).get(15, TimeUnit.SECONDS);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("groupId", group.groupId());
            result.put("state", group.state().name());
            result.put("members", group.members().stream().map(member -> Map.of(
                    "consumerId", member.consumerId(),
                    "clientId", member.clientId(),
                    "host", member.host(),
                    "assignments", member.assignment().topicPartitions().stream()
                            .map(tp -> Map.of("topic", tp.topic(), "partition", tp.partition()))
                            .toList()
            )).toList());
            return result;
        }
    }
}
