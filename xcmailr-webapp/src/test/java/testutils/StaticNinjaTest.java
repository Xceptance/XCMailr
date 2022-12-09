/*
 *  Copyright 2020 by the original author or authors.
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
 */
package testutils;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import ninja.NinjaTest;
import ninja.utils.NinjaTestBrowser;
import ninja.utils.NinjaTestServer;

/**
 * Base class for tests that require a running server. Similar to {@link NinjaTest}, but starts/stops the Ninja server
 * once per test class instead of once per test. This can reduce test execution time significantly.
 */
public class StaticNinjaTest
{
    /** Backend of the test => Starts Ninja */
    protected static NinjaTestServer ninjaTestServer;

    /** A persistent HttpClient that stores cookies to make requests */
    protected static NinjaTestBrowser ninjaTestBrowser;

    @BeforeClass
    public static void startupServerAndBrowser()
    {
        ninjaTestServer = NinjaTestServer.builder().build();
        ninjaTestBrowser = new NinjaTestBrowser();
    }

    @AfterClass
    public static void shutdownServerAndBrowser()
    {
        ninjaTestServer.shutdown();
        ninjaTestBrowser.shutdown();
    }
}
