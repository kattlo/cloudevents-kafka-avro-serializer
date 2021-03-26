package io.github.kattlo.cloudevents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.Test;

import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.kafka.CloudEventSerializer;
import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import org.acme.AvroEventDataExample;

public class KafkaAvroCloudEventDeserializerTest {

    @Test
    public void should_deserialize_as_specific_record() {

        // setup
        var topico = "meu-topico-des";

        var expected = new AvroEventDataExample(130l, "Nome 130", "Descrição 130");
        var data = new AvroCloudEventData<>(expected);

        var registry = new MockSchemaRegistryClient();
        var serializer = new KafkaAvroCloudEventSerializer(registry);
        var deserializer = new KafkaAvroCloudEventDeserializer(registry);

        Map<String, Object> configs = new HashMap<>();

        configs.put(CloudEventSerializer.ENCODING_CONFIG, "BINARY");
        configs.put(KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS, "true");
        configs.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG,
            "http://localhost:8081");

        configs.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, "true");

        serializer.configure(configs, Boolean.FALSE);
        deserializer.configure(configs, Boolean.FALSE);

        var evento = CloudEventBuilder.v1()
            .withId(UUID.randomUUID().toString())
            .withSource(URI.create("/exemplo/enviar"))
            .withType(expected.getClass().getName())
            .withTime(OffsetDateTime.now())
            .withData(AvroCloudEventData.MIME_TYPE, data)
            .build();

        Headers headers = new RecordHeaders();
        var bytes = serializer.serialize(topico, headers, evento);

        // act
        var actual = deserializer.deserialize(topico, headers, bytes);

        // assert
        var actualData = actual.getData();
        assertTrue(actualData instanceof AvroCloudEventData);

        @SuppressWarnings("unchecked")
        var avroData = (AvroCloudEventData<AvroEventDataExample>)actualData;

        var actualValue = avroData.getValue();
        assertEquals(expected, actualValue);

        serializer.close();
        deserializer.close();
    }

    @Test
    public void should_deserialize_as_generic_record() {

        // setup
        var topico = "meu-topico-des";

        var expected = new AvroEventDataExample(130l, "Nome 130", "Descrição 130");
        var data = new AvroCloudEventData<>(expected);

        var registry = new MockSchemaRegistryClient();
        var serializer = new KafkaAvroCloudEventSerializer(registry);
        var deserializer = new KafkaAvroCloudEventDeserializer(registry);

        Map<String, Object> configs = new HashMap<>();

        configs.put(CloudEventSerializer.ENCODING_CONFIG, "BINARY");
        configs.put(KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS, "true");
        configs.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG,
            "http://localhost:8081");

        configs.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, "false");

        serializer.configure(configs, Boolean.FALSE);
        deserializer.configure(configs, Boolean.FALSE);

        var evento = CloudEventBuilder.v1()
            .withId(UUID.randomUUID().toString())
            .withSource(URI.create("/exemplo/enviar"))
            .withType(expected.getClass().getName())
            .withTime(OffsetDateTime.now())
            .withData(AvroCloudEventData.MIME_TYPE, data)
            .build();

        Headers headers = new RecordHeaders();
        var bytes = serializer.serialize(topico, headers, evento);

        // act
        var actual = deserializer.deserialize(topico, headers, bytes);

        // assert
        var actualData = actual.getData();
        assertTrue(actualData instanceof AvroCloudEventData);

        @SuppressWarnings("unchecked")
        var avroData = (AvroCloudEventData<GenericRecord>)actualData;

        var actualValue = avroData.getValue();
        assertEquals(expected.getCode(), actualValue.get("code"));
        assertEquals(expected.getName(), actualValue.get("name").toString());
        assertEquals(expected.getDescription(), actualValue.get("description").toString());

        serializer.close();
        deserializer.close();
    }

    @Test
    public void should_deserialize_ce_attributes() {

        // setup
        var topico = "meu-topico-des";

        var value = new AvroEventDataExample(130l, "Nome 130", "Descrição 130");
        var data = new AvroCloudEventData<>(value);

        var registry = new MockSchemaRegistryClient();
        var serializer = new KafkaAvroCloudEventSerializer(registry);
        var deserializer = new KafkaAvroCloudEventDeserializer(registry);

        Map<String, Object> configs = new HashMap<>();

        configs.put(CloudEventSerializer.ENCODING_CONFIG, "BINARY");
        configs.put(KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS, "true");
        configs.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG,
            "http://localhost:8081");

        configs.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, "false");

        serializer.configure(configs, Boolean.FALSE);
        deserializer.configure(configs, Boolean.FALSE);

        var expected = CloudEventBuilder.v1()
            .withId(UUID.randomUUID().toString())
            .withSource(URI.create("/exemplo/enviar"))
            .withType(value.getClass().getName())
            .withTime(OffsetDateTime.now())
            .withData(AvroCloudEventData.MIME_TYPE, data)
            .build();

        Headers headers = new RecordHeaders();
        var bytes = serializer.serialize(topico, headers, expected);

        // act
        var actual = deserializer.deserialize(topico, headers, bytes);

        // assert
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getSource(), actual.getSource());
        assertEquals(expected.getType(), actual.getType());
        assertEquals(expected.getTime(), actual.getTime());
        assertNotNull(actual.getDataSchema());

        serializer.close();
        deserializer.close();
    }
}
