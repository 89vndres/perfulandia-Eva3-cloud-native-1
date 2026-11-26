
package com.perfulandia.backend.messaging.kafka.config;

import com.perfulandia.backend.messaging.kafka.dto.OrdenEvento;
import com.perfulandia.backend.messaging.kafka.dto.FacturacionEvento;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaListenerConfig {

    private static final Logger log =
            LoggerFactory.getLogger(KafkaListenerConfig.class);

    private Map<String, Object> consumerProperties(KafkaProperties kafkaProperties) {
        Map<String, Object> props =
                new HashMap<>(kafkaProperties.buildConsumerProperties());

        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return props;
    }

    @Bean
    public ConsumerFactory<String, OrdenEvento> ordenConsumerFactory(
            KafkaProperties kafkaProperties) {

        JsonDeserializer<OrdenEvento> jsonDeserializer =
                new JsonDeserializer<>(OrdenEvento.class, false);

        ErrorHandlingDeserializer<OrdenEvento> valueDeserializer =
                new ErrorHandlingDeserializer<>(jsonDeserializer);

        return new DefaultKafkaConsumerFactory<>(
                consumerProperties(kafkaProperties),
                new StringDeserializer(),
                valueDeserializer
        );
    }

    @Bean
    public ConsumerFactory<String, FacturacionEvento> facturacionConsumerFactory(
            KafkaProperties kafkaProperties) {

        JsonDeserializer<FacturacionEvento> jsonDeserializer =
                new JsonDeserializer<>(FacturacionEvento.class, false);

        ErrorHandlingDeserializer<FacturacionEvento> valueDeserializer =
                new ErrorHandlingDeserializer<>(jsonDeserializer);

        return new DefaultKafkaConsumerFactory<>(
                consumerProperties(kafkaProperties),
                new StringDeserializer(),
                valueDeserializer
        );
    }

    @Bean
    public CommonErrorHandler kafkaCommonErrorHandler() {
        return new DefaultErrorHandler(
                (record, exception) -> log.error(
                        "Evento Kafka descartado tras agotar reintentos. topic={}, partition={}, offset={}, key={}",
                        record.topic(),
                        record.partition(),
                        record.offset(),
                        record.key(),
                        exception
                ),
                new FixedBackOff(1000L, 2L)
        );
    }

    @Bean(name = "ordenKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, OrdenEvento>
    ordenKafkaListenerContainerFactory(
            ConsumerFactory<String, OrdenEvento> ordenConsumerFactory,
            CommonErrorHandler kafkaCommonErrorHandler) {

        ConcurrentKafkaListenerContainerFactory<String, OrdenEvento> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(ordenConsumerFactory);
        factory.setCommonErrorHandler(kafkaCommonErrorHandler);

        return factory;
    }

    @Bean(name = "facturacionKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, FacturacionEvento>
    facturacionKafkaListenerContainerFactory(
            ConsumerFactory<String, FacturacionEvento> facturacionConsumerFactory,
            CommonErrorHandler kafkaCommonErrorHandler) {

        ConcurrentKafkaListenerContainerFactory<String, FacturacionEvento> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(facturacionConsumerFactory);
        factory.setCommonErrorHandler(kafkaCommonErrorHandler);

        return factory;
    }
}
