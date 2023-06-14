/*
 * Copyright (c) 2013-2023 Xceptance Software Technologies GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
