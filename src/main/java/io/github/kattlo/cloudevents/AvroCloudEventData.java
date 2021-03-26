package io.github.kattlo.cloudevents;

import java.util.Objects;

import org.apache.avro.generic.IndexedRecord;

import io.cloudevents.CloudEventData;
import io.cloudevents.core.message.Encoding;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Avro CloudEvent data to use with {@link Encoding#BINARY}
 * @author fabiojose
 */
@ToString
@EqualsAndHashCode
@Getter
public class AvroCloudEventData<T extends IndexedRecord>
implements
    CloudEventData {

    public static final String MIME_TYPE = "application/avro";

    public final T value;
    public AvroCloudEventData(final T value){
        this.value = Objects.requireNonNull(value);
    }

    @Override
    public byte[] toBytes() {
        return new byte[]{};
    }

    /**
     * Parse untyped data to typed one.
     *
     * @param <T> The type of the typed data
     * @param data Instance of {@link AvroCloudEventData}
     * @return typed data
     */
    @SuppressWarnings("unchecked")
    public static <V extends IndexedRecord> V dataOf(CloudEventData data) {

        if(data instanceof AvroCloudEventData){

            return ((AvroCloudEventData<V>)data).getValue();

        } else {
            throw new IllegalArgumentException("data argument must be an instance of "
                + AvroCloudEventData.class.getName());
        }
    }
}
