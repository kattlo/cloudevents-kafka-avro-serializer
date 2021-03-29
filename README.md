# 🌤️ CloudEvents Apache Kafka® - Apache Avro™ Serialization

Serialize and Deserialize [CloudEvents](https://cloudevents.io/) integrated
with [Schema Registry](https://docs.confluent.io/platform/current/schema-registry/index.html).

- 💡 [See examples](./examples)

__Support__:

- Java 11+
- Apache Kafka® 2.6.0+
- CloudEvents [Spec v1.0.1](https://github.com/cloudevents/spec/blob/v1.0.1/spec.md)
- CloudEvents [Binary Content Mode](https://github.com/cloudevents/spec/blob/v1.0.1/kafka-protocol-binding.md#32-binary-content-mode)

## Getting Started

1. Dependency

  - Gradle
    ```groovy
    repositories {
        // ...
        maven { url 'https://jitpack.io' }
    }

    dependencies {
	    testImplementation 'com.github.kattlo:cloudevents-kafka-avro-serializer:v0.10.0'
	}
    ```

  - Apache Maven®
    ```xml
    <repositories>
		<repository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</repository>
	</repositories>

	<dependency>
	    <groupId>com.github.kattlo</groupId>
	    <artifactId>cloudevents-kafka-avro-serializer</artifactId>
	    <version>v0.10.0</version>
	</dependency>
    ```

2. Configure
  - Serializer
    ```properties
    cloudevents.serializer.encoding=BINARY
    schema.registry.url=http://configure.me:8081
    auto.register.schemas=true

    value.serializer=io.github.kattlo.cloudevents.KafkaAvroCloudEventSerializer
    ```
  - Deserializer
    ```properties
    specific.avro.reader=false #to use GenericRecord data
    #specific.avro.reader=true #to use strong typed data
    schema.registry.url=http://configure.me:8081

    value.deserializer=io.github.kattlo.cloudevents.KafkaAvroCloudEventDeserializer
    ```

3. Use
  - Serialization
    ```java
    import java.net.URI;
    import java.time.OffsetDateTime;
    import java.util.UUID;
    import io.cloudevents.core.builder.CloudEventBuilder;
    import io.github.kattlo.cloudevents.AvroCloudEventData;
    import org.apache.kafka.clients.producer.ProducerRecord;

    // . . .

    var event = CloudEventBuilder
        .v1()
        .withId(UUID.randomUUID().toString())
        .withSource(URI.create("/example"))
        .withType("type.example")
        .withTime(OffsetDateTime.now())
        .withData(AvroCloudEventData.MIME_TYPE, data)
        .build();

    var record = new ProducerRecord<>("my-topic", event);

    // --- create KafkaProducer with Serializer configurations --- //

    // producer.send(record);
    ```
  - Deserialization
    ```java
    import io.github.kattlo.cloudevents.AvroCloudEventData;
    import io.cloudevents.CloudEvent;
    import org.apache.avro.generic.GenericRecord;

    // --- create KafkaConsumer with Deserializer configurations --- //

    // consumer.subscribe(...)

    //var records = consumer.pool()

    records.forEach(record -> {

      // Get the CloudEvent instance
      CloudEvent event = record.value();

      // when specific.avro.reader=false
      GenericRecord data = AvroCloudEventData.dataOf(event);

      // when specific.avro.reader=true
      YourType data = AvroCloudEventData.dataOf(event);

    });
    ```
