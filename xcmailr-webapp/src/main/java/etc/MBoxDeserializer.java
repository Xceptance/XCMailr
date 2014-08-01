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
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class MBoxDeserializer extends JsonDeserializer<MBox> implements ContextualDeserializer
{

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctx, BeanProperty property)
        throws JsonMappingException
    {
        // TODO Auto-generated method stub
        return  new MBoxIdSimpleDeserializer();
    }

    @Override
    public MBox deserialize(JsonParser jpars, DeserializationContext ctx) throws IOException, JsonProcessingException
    {
        MBox tmp = new MBox();
        JsonNode node = jpars.getCodec().readTree(jpars);
        tmp.setId(((IntNode)node.get("id")).longValue());
        tmp.setAddress(((TextNode)node.get("address")).textValue());
        tmp.setTs_Active(((IntNode)node.get("ts_Active")).longValue());
        tmp.setExpired(((BooleanNode)node.get("expired")).booleanValue());
        tmp.setDomain(((TextNode)node.get("domain")).textValue());
        tmp.setSuppressions(((IntNode)node.get("suppressions")).intValue());
        tmp.setForwards(((IntNode)node.get("forwards")).intValue());
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
            long id = ((NumericNode)node.get("id")).longValue();
            long ts = ((NumericNode)node.get("ts_Active")).longValue();
            long tsDt = HelperUtils.parseTimeString(((TextNode)node.get("datetime")).textValue());
            
            String address = ((TextNode)node.get("address")).textValue();
            String domain = ((TextNode)node.get("domain")).textValue();
            boolean expired =((BooleanNode)node.get("expired")).booleanValue();
            int suppressions =((NumericNode)node.get("suppressions")).intValue();
            int forwards =((NumericNode)node.get("forwards")).intValue() ;
            System.out.println("id:"+id+" ts: "+ts+" address:"+address+" domain:"+domain+" exp:"+expired+" supps:"+suppressions+" fwds:"+forwards);
            tmp.setId(id);
            tmp.setAddress(address);
            tmp.setTs_Active(tsDt);
            tmp.setExpired(expired);
            tmp.setDomain(domain);
            tmp.setSuppressions(suppressions);
            tmp.setForwards(forwards);
            return tmp;
        }
    }
}
