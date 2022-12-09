package conf;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import ninja.lifecycle.Start;

/**
 * <p>
 * Configures the object mapper used by Jackson.
 * </p>
 * <p>
 * En- or disables certain serialization and/or de-serialization features of Jackson:
 * <ul>
 * <li>ignore unknown properties when deserializing JSON</li>
 * </ul>
 * </p>
 */
@Singleton
public class JacksonSetup
{
    private final ObjectMapper objectMapper;

    @Inject
    public JacksonSetup(final ObjectMapper aObjectMapper)
    {
        this.objectMapper = aObjectMapper;
    }

    @Start(order = 90)
    public void configureObjectMapper()
    {
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
