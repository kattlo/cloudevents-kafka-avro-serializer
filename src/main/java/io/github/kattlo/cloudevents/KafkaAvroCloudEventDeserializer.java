package io.github.kattlo.cloudevents;

import java.util.Map;

import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.common.header.Headers;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.message.Encoding;
import io.cloudevents.kafka.CloudEventDeserializer;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import lombok.extern.slf4j.Slf4j;

/**
 * Just the {@link Encoding#BINARY} and Spec 1.0 are supported.
 *
 * @author fabiojose
 */
@Slf4j
public class KafkaAvroCloudEventDeserializer extends KafkaAvroDeserializer {

    private CloudEventDeserializer ce = new CloudEventDeserializer();

    public KafkaAvroCloudEventDeserializer() {
    }

    public KafkaAvroCloudEventDeserializer(SchemaRegistryClient registry) {
        super(registry);
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        log.debug("deserializer configurations {}", configs);

        super.configure(configs, isKey);
        ce.configure(configs, isKey);
    }

    @Override
    public CloudEvent deserialize(String topic, Headers headers, byte[] bytes) {

        var value = super.deserialize(topic, headers, bytes);
        var data = new AvroCloudEventData<GenericRecord>((GenericRecord)value);

        var event = ce.deserialize(topic, headers, bytes);

        var result = CloudEventBuilder
            .from(event)
            .withData(AvroCloudEventData.MIME_TYPE, data)
            .build();

        return result;
    }
}
