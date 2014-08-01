package etc;

import java.io.IOException;

import models.MBox;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class MBoxSerializer extends JsonSerializer<MBox> implements ContextualSerializer
{

    public void serialize(MBox value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonProcessingException
    {
    }

    @Override
    public void serializeWithType(MBox value, JsonGenerator jgen, SerializerProvider provider, TypeSerializer typeSer)
        throws IOException, JsonProcessingException
    {
        serialize(value, jgen, provider);
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property)
        throws JsonMappingException
    {
        return new MBoxIdSimpleSerializer();
    }

    public class MBoxIdSimpleSerializer extends StdSerializer<MBox>
    {

        public MBoxIdSimpleSerializer()
        {
            super(MBox.class);
        }

        @Override
        public void serialize(MBox value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeStartObject();
            writeFieldAndValue(jgen, "id", value.getId());
            writeFieldAndValue(jgen, "address", value.getAddress());
            writeFieldAndValue(jgen, "ts_Active", value.getTs_Active());
            writeFieldAndValue(jgen, "expired", value.isExpired());
            writeFieldAndValue(jgen, "domain", value.getDomain());
            writeFieldAndValue(jgen, "forwards", value.getForwards());
            writeFieldAndValue(jgen, "suppressions", value.getSuppressions());
            writeFieldAndValue(jgen, "datetime", value.getDatetime());
            writeFieldAndValue(jgen, "fullAddress", value.getFullAddress());
            jgen.writeEndObject();
            
        }

        private void writeFieldAndValue(JsonGenerator jgen, String fieldName, String value) throws JsonGenerationException, IOException
        {
            jgen.writeFieldName(fieldName);
            jgen.writeString(value);
        }
        private void writeFieldAndValue(JsonGenerator jgen, String fieldName, long value) throws JsonGenerationException, IOException
        {
            jgen.writeFieldName(fieldName);
            jgen.writeNumber(value);
        }
        private void writeFieldAndValue(JsonGenerator jgen, String fieldName, boolean value) throws JsonGenerationException, IOException
        {
            jgen.writeFieldName(fieldName);
            jgen.writeBoolean(value);
        }

        @Override
        public void serializeWithType(MBox value, JsonGenerator jgen, SerializerProvider provider,
                                      TypeSerializer typeSer) throws IOException, JsonProcessingException
        {
            serialize(value, jgen, provider);
        }
    }

    // public class BaseEntityIdCollectionSerializer extends StdSerializer<Collection<? extends AbstractData>>
    // {
    //
    // public BaseEntityIdCollectionSerializer()
    // {
    // super(Collection.class, false);
    // }
    //
    // @Override
    // public void serialize(Collection<? extends AbstractData> value, JsonGenerator jgen, SerializerProvider provider)
    // throws IOException, JsonGenerationException
    // {
    // jgen.writeStartArray();
    // for (AbstractData b : value)
    // {
    // jgen.writeNumber(b.getId());
    // }
    // jgen.writeEndArray();
    // }
    //
    // @Override
    // public void serializeWithType(Collection<? extends AbstractData> value, JsonGenerator jgen,
    // SerializerProvider provider, TypeSerializer typeSer)
    // throws IOException, JsonProcessingException
    // {
    // serialize(value, jgen, provider);
    // }
    // }
}
