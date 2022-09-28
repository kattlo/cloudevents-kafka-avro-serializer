package io.github.kattlo.cloudevents;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.header.Headers;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.message.Encoding;
import io.cloudevents.kafka.CloudEventSerializer;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.entities.SchemaReference;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import io.confluent.kafka.serializers.subject.strategy.SubjectNameStrategy;
import lombok.extern.slf4j.Slf4j;

/**
 * Just the {@link Encoding#BINARY} and Spec 1.0 are supported.
 *
 * @author fabiojose
 */
@Slf4j
public class KafkaAvroCloudEventSerializer extends KafkaAvroSerializer {

    public static final String DATASCHEMA_HEADER = "ce_dataschema";

    private final CloudEventSerializer ce = new CloudEventSerializer();
    private String schemaRegistryUrl;

    public KafkaAvroCloudEventSerializer() {
    }

    public KafkaAvroCloudEventSerializer(SchemaRegistryClient client) {
        super(client);
    }

    private Encoding encodingOf(Map<String, ?> configs){
        log.debug("serializer configurations {}", configs);

        Object encodingConfig = configs.get(CloudEventSerializer.ENCODING_CONFIG);
        Encoding encoding = null;

        if (encodingConfig instanceof String) {
            encoding = Encoding.valueOf((String) encodingConfig);
        } else if (encodingConfig instanceof Encoding) {
            encoding = (Encoding) encodingConfig;
        } else if (encodingConfig != null) {
            throw new IllegalArgumentException(CloudEventSerializer.ENCODING_CONFIG + " can be of type String or " + Encoding.class.getCanonicalName());
        }

        return encoding;
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        var encoding = encodingOf(configs);

        if(encoding == Encoding.BINARY){

            super.configure(configs, isKey);
            ce.configure(configs, isKey);

            schemaRegistryUrl = (String)
                configs.get(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG);

            log.debug("{}={}", KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG,
                schemaRegistryUrl);;

        } else {
            throw new IllegalArgumentException(CloudEventSerializer.ENCODING_CONFIG + "=" + encoding + " not supported");
        }
    }

    @Override
    public byte[] serialize(String topic, Headers headers, Object event) {
        if( !(event instanceof CloudEvent)){
            throw new IllegalArgumentException("event argument must be an instance of " + CloudEvent.class);
        }

        final var ceEvent = (CloudEvent)event;

        ce.serialize(topic, headers, ceEvent);
        log.debug("CloudEvent headers {}", headers);


        if(ceEvent.getData() instanceof AvroCloudEventData) {
            var data = (AvroCloudEventData<?>)ceEvent.getData();
            var value = data.getValue();
            var valueType = value.getClass();
            log.debug("value to serialize as avro {}", value);

            // serialize CloudEvent data and register the schema
            var bytes = super.serialize(topic, headers, data.getValue());

            // use the strategy to create the subject name
            var strategy = (SubjectNameStrategy)super.valueSubjectNameStrategy;
            log.debug("SubjectNameStrategy {}", strategy);

            var subjectName = strategy.subjectName(topic, Boolean.FALSE,
                new NoSchema(valueType.getPackageName(), valueType.getSimpleName()));

            log.info("SubjectName {}", subjectName);

            // get the versionId of registered schema
            try {
                var versions = super.schemaRegistry.getAllVersions(subjectName);
                var version = versions.get(versions.size() -1);
                log.debug("Schema versionId {}", version);

                var dataschema = schemaRegistryUrl + "/subjects/" + subjectName + "/versions/" + version + "/schema";
                log.debug("{}={}", DATASCHEMA_HEADER, dataschema);

                headers.remove(DATASCHEMA_HEADER);
                headers.add(DATASCHEMA_HEADER, dataschema.getBytes());

            }catch(IOException | RestClientException e){
                throw new SerializationException(e.getMessage(), e);
            }

            return bytes;

        } else {
            throw new IllegalArgumentException("CloudEvent data attribute must be an instance of "
                + AvroCloudEventData.class.getName());
        }
    }

    private static final class NoSchema implements ParsedSchema {

        private final String namespace;
        private final String name;

        public NoSchema(String namespace, String name) {
            this.namespace = Objects.requireNonNull(namespace);
            this.name = Objects.requireNonNull(name);
        }

        @Override
        public String canonicalString() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<String> isBackwardCompatible(ParsedSchema arg0) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String name() {
            return namespace + "." + name;
        }

        @Override
        public Object rawSchema() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<SchemaReference> references() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String schemaType() {
            throw new UnsupportedOperationException();
        }

    }
}
