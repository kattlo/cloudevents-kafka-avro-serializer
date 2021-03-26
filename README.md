# CloudEvents Apache Kafka® - Apache Avro™ Serialization

Serialize and Deserialize [CloudEvents](https://cloudevents.io/) integrated
with [Schema Registry](https://docs.confluent.io/platform/current/schema-registry/index.html).

__Support__:

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
	    testImplementation 'com.github.kattlo:cloudevents-kafka-avro-serializer:v0.9.0'
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
	    <version>v0.9.0</version>
	</dependency>
    ```

2. Configure
```properties

```

3. Use
```java

```
