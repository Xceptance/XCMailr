package etc;

import java.io.IOException;

import models.MBox;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.TextNode;

public class MBoxDeserializer extends JsonDeserializer<MBox> implements ContextualDeserializer
{

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctx, BeanProperty property)
        throws JsonMappingException
    {
        return new MBoxIdSimpleDeserializer();
    }

    @Override
    public MBox deserialize(JsonParser jpars, DeserializationContext ctx) throws IOException, JsonProcessingException
    {
        MBox tmp = new MBox();
        JsonNode node = jpars.getCodec().readTree(jpars);

        long tsDt = HelperUtils.parseTimeString(((TextNode) node.get("datetime")).textValue());
        String address = null;
        String domain = null;
        if (node.hasNonNull("address"))
            address = ((TextNode) node.get("address")).textValue();
        if (node.hasNonNull("domain"))
            domain = ((TextNode) node.get("domain")).textValue();
        tmp.setAddress(address);
        tmp.setTs_Active(tsDt);
        tmp.setDomain(domain);
        return tmp;
    }

    public class MBoxIdSimpleDeserializer extends StdDeserializer<MBox>
    {

        public MBoxIdSimpleDeserializer()
        {
            super(MBox.class);
        }

        @Override
        public MBox deserialize(JsonParser jpars, DeserializationContext ctx)
            throws IOException, JsonProcessingException
        {
            MBox tmp = new MBox();
            JsonNode node = jpars.getCodec().readTree(jpars);

            long tsDt = 0;
            if (node.hasNonNull("datetime"))
                tsDt = HelperUtils.parseTimeString(((TextNode) node.get("datetime")).textValue());
            String address = null;
            String domain = null;
            if (node.hasNonNull("address"))
                address = ((TextNode) node.get("address")).textValue();
            if (node.hasNonNull("domain"))
                domain = ((TextNode) node.get("domain")).textValue();
            tmp.setAddress(address);
            tmp.setTs_Active(tsDt);
            tmp.setDomain(domain);
            return tmp;
        }
    }
}
