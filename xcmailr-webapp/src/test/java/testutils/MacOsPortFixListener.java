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
package testutils;

import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;

/**
 * JUnit 4 RunListener that binds privileged ports 1000-1023 on macOS to prevent
 * Ninja Framework's StandaloneHelper from selecting ports below 1024.
 *
 * On macOS, non-root processes can bind 0.0.0.0:1000 but cannot bind 127.0.0.1:1000.
 * Holding ports 1000-1023 forces findAvailablePort(1000, 10000) to select port >= 1024.
 */
public final class MacOsPortFixListener extends RunListener
{
    private static final List<ServerSocket> DUMMY_SOCKETS = new ArrayList<>();

    static
    {
        for (int port = 1000; port < 1024; port++)
        {
            try
            {
                DUMMY_SOCKETS.add(new ServerSocket(port));
            }
            catch (final Exception ignored)
            {
            }
        }
    }

    @Override
    public void testRunStarted(final Description description) throws Exception
    {
        // Static initializer already executed
    }
}
