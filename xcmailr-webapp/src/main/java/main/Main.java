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
package main;

import org.eclipse.jetty.server.handler.gzip.GzipHandler;

/**
 * Main entry point for application.
 */
public class Main
{
    public static void main(String[] args)
    {
        new NinjaJetty().run();
    }

    private static class NinjaJetty extends ninja.standalone.NinjaJetty
    {
        @Override
        protected void doConfigure() throws Exception
        {
            super.doConfigure();

            // add GZIP support to Jetty
            final GzipHandler gzipHandler = new GzipHandler();
            jetty.insertHandler(gzipHandler);
        }
    }
}
