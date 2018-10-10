package etc;

import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;

public abstract class TypeRef
{
    public static final TypeReference<Map<String, Boolean>> MAP_STRING_BOOLEAN = new TypeReference<Map<String, Boolean>>()
    {
    };

}
