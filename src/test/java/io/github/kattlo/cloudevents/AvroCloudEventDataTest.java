package io.github.kattlo.cloudevents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.cloudevents.core.builder.CloudEventBuilder;
import org.acme.AvroEventDataExample;

public class AvroCloudEventDataTest {

    @Test
    public void should_return_the_actual_data_value() {

        var evento = new AvroEventDataExample(300l, "Nome 300", "Descrição 300");
        var data = new AvroCloudEventData<AvroEventDataExample>(evento);

        assertEquals(evento, data.getValue());
    }

    @Test
    public void should_build_cloud_event_v1_with_avro() {

        var valor = new AvroEventDataExample(300l, "Nome 300", "Descrição 300");
        var data = new AvroCloudEventData<>(valor);

        var evento = CloudEventBuilder
            .v1()
            .withId(UUID.randomUUID().toString())
            .withSource(URI.create("/exemplo/enviar"))
            .withType(valor.getClass().getName())
            .withTime(OffsetDateTime.now())
            .withData(AvroCloudEventData.MIME_TYPE, data)
            .build();

        assertEquals(data, evento.getData());
    }

    @Test
    public void should_return_data_value_as_bytes() {

        var valor = new AvroEventDataExample(300l, "Nome 300", "Descrição 300");
        var data = new AvroCloudEventData<>(valor);

        var actual = data.toBytes();

        assertNotNull(actual);
    }

}
