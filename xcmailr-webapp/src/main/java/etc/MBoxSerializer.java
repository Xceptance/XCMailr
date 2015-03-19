/**  
 *  Copyright 2013 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 *
 */
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
//TODO 
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
}
