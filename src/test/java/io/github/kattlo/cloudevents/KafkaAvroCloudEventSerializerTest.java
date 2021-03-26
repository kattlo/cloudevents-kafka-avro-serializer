package io.github.kattlo.cloudevents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.Test;

import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.kafka.CloudEventSerializer;
import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import org.acme.AvroEventDataExample;

public class KafkaAvroCloudEventSerializerTest {

    @Test
    public void should_throw_when_encoding_is_not_binary() {

        // setup
        Map<String, Object> configs = new HashMap<>();

        configs.put(CloudEventSerializer.ENCODING_CONFIG, "STRUCTURED");

        configs.put(KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS, "true");
        configs.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG,
            "http://localhost:8081");

        // act
        var serializer = new KafkaAvroCloudEventSerializer();

        var actual = assertThrows(IllegalArgumentException.class, () ->
            serializer.configure(configs, Boolean.FALSE));

        serializer.close();
        assertTrue(actual.getMessage().contains("not supported"));
    }

    @Test
    public void should_initialize(){

        // setup
        Map<String, Object> configs = new HashMap<>();

        configs.put(CloudEventSerializer.ENCODING_CONFIG, "BINARY");

        configs.put(KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS, "true");
        configs.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG,
            "http://localhost:8081");

        // act
        var serializer = new KafkaAvroCloudEventSerializer();
        serializer.configure(configs, Boolean.FALSE);

        serializer.close();
    }

    @Test
    public void should_register_the_schema_of_serialized_value() throws Exception {

        // setup
        var topico = "meu-topico";
        var subject = topico + "-value";

        var registry = new MockSchemaRegistryClient();
        var serializer = new KafkaAvroCloudEventSerializer(registry);

        Map<String, Object> configs = new HashMap<>();

        configs.put(CloudEventSerializer.ENCODING_CONFIG, "BINARY");

        configs.put(KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS, "true");
        configs.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG,
            "http://localhost:8081");

        serializer.configure(configs, Boolean.FALSE);

        var valor = new AvroEventDataExample(300l, "Nome 300", "Descrição 300");
        var data = new AvroCloudEventData<>(valor);

        var evento = CloudEventBuilder
            .v1()
            .withId(UUID.randomUUID().toString())
            .withSource(URI.create("/exemplo/enviar"))
            .withType(valor.getClass().getName())
            .withTime(OffsetDateTime.now())
            .withData("application/avro", data)
            .build();

        //
        Headers headers = new RecordHeaders();
        var serialized = serializer.serialize(topico, headers, evento);

        var versions = registry.getAllVersions(subject);

        // assert
        assertNotNull(serialized);
        assertEquals(1, versions.size());

        var response = registry.getByVersion(subject, versions.iterator().next(), false);

        var actual = registry.parseSchema(
            response.getSchemaType(),
            response.getSchema(),
            response.getReferences()
        );
        assertTrue(actual.isPresent());

        assertEquals(valor.getSchema(), actual.get().rawSchema());

        serializer.close();
    }

    @Test
    public void should_fill_the_headers() {

        // setup
        var topico = "meu-topico";

        var registry = new MockSchemaRegistryClient();
        var serializer = new KafkaAvroCloudEventSerializer(registry);

        Map<String, Object> configs = new HashMap<>();

        configs.put(CloudEventSerializer.ENCODING_CONFIG, "BINARY");

        configs.put(KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS, "true");
        configs.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG,
            "http://localhost:8081");

        serializer.configure(configs, Boolean.FALSE);

        var valor = new AvroEventDataExample(300l, "Nome 300", "Descrição 300");
        var data = new AvroCloudEventData<>(valor);

        var evento = CloudEventBuilder
            .v1()
            .withId(UUID.randomUUID().toString())
            .withSource(URI.create("/exemplo/enviar"))
            .withType(valor.getClass().getName())
            .withTime(OffsetDateTime.now())
            .withData("application/avro", data)
            .build();

        //
        Headers headers = new RecordHeaders();
        serializer.serialize(topico, headers, evento);

        assertTrue(headers.iterator().hasNext());

        assertEquals("1.0", new String(headers.lastHeader("ce_specversion").value()));
        assertEquals(evento.getId(), new String(headers.lastHeader("ce_id").value()));
        assertEquals(evento.getSource().toString(), new String(headers.lastHeader("ce_source").value()));
        assertEquals(evento.getType(), new String(headers.lastHeader("ce_type").value()));
        assertEquals(evento.getDataContentType(), new String(headers.lastHeader("content-type").value()));

        serializer.close();
    }

    @Test
    public void should_fill_the_ce_dataschema_with_right_reference() {

        ///subjects/{topic-name-value}/versions/(versionId: version)/schema
        // setup
        var topico = "meu-topico";
        var expected = "http://localhost:8081/subjects/" + topico + "-value/versions/1/schema";

        var registry = new MockSchemaRegistryClient();
        var serializer = new KafkaAvroCloudEventSerializer(registry);

        Map<String, Object> configs = new HashMap<>();

        configs.put(CloudEventSerializer.ENCODING_CONFIG, "BINARY");

        configs.put(KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS, "true");
        configs.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG,
            "http://localhost:8081");

        serializer.configure(configs, Boolean.FALSE);

        var valor = new AvroEventDataExample(300l, "Nome 300", "Descrição 300");
        var data = new AvroCloudEventData<>(valor);

        var evento = CloudEventBuilder
            .v1()
            .withId(UUID.randomUUID().toString())
            .withSource(URI.create("/exemplo/enviar"))
            .withType(valor.getClass().getName())
            .withTime(OffsetDateTime.now())
            .withData("application/avro", data)
            .build();

        //
        Headers headers = new RecordHeaders();
        serializer.serialize(topico, headers, evento);

        assertTrue(headers.iterator().hasNext());

        var header = headers.lastHeader(KafkaAvroCloudEventSerializer.DATASCHEMA_HEADER);
        assertNotNull(header);

        var actual = new String(header.value());
        assertEquals(expected, actual);

        serializer.close();
    }
}
