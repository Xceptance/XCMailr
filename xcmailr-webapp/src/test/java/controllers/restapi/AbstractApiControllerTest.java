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
package controllers.restapi;

import org.apache.http.HttpResponse;
import org.junit.Before;
import org.junit.Test;

import controllers.restapi.util.AbstractApiController;
import models.User;
import testutils.RestApiClient;
import testutils.RestApiTestUtils;
import testutils.StaticNinjaTest;
import testutils.TestDataUtils;

/**
 * Checks the functionality that is common to REST API controllers that extend {@link AbstractApiController}.
 */
public class AbstractApiControllerTest extends StaticNinjaTest
{
    private RestApiClient apiClient;

    @Before
    public void before()
    {
        final User user = TestDataUtils.createUser();
        apiClient = new RestApiClient(ninjaTestServer, ninjaTestBrowser, user.getApiToken());
    }

    // -----------------------------------------------------------

    /**
     * Checks that access to the API is possible with a valid API token.
     */
    @Test
    public void accessWithValidApiToken() throws Exception
    {
        final HttpResponse response = apiClient.listMailboxes();

        RestApiTestUtils.validateStatusCode(response, 200);
    }

    /**
     * Checks that access to the API is not possible with an invalid API token.
     */
    @Test
    public void accessWithInvalidApiToken() throws Exception
    {
        // wrong token
        apiClient.setApiToken("wrongApiToken");
        HttpResponse response = apiClient.listMailboxes();

        RestApiTestUtils.validateStatusCode(response, 401); // Unauthorized

        // empty token
        apiClient.setApiToken("");
        response = apiClient.listMailboxes();

        RestApiTestUtils.validateStatusCode(response, 401); // Unauthorized
        
        // no token
        apiClient.setApiToken(null);
        response = apiClient.listMailboxes();

        RestApiTestUtils.validateStatusCode(response, 401); // Unauthorized
    }
}
