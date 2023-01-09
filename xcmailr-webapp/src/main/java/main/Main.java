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
