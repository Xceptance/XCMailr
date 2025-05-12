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
package xcmailr.loadtesting.util;

import java.util.Properties;

import javax.mail.Session;

import com.xceptance.xlt.api.engine.ActionData;
import com.xceptance.xlt.api.engine.GlobalClock;
import com.xceptance.xlt.api.util.XltProperties;
import com.xceptance.xlt.api.util.XltRandom;
import com.xceptance.xlt.engine.SessionImpl;
import com.xceptance.xlt.engine.util.TimerUtils;

import xcmailr.client.XCMailrClient;

/**
 * A collection of utility methods used throughout the test suite.
 */
public class Utils
{
    private static final XCMailrClient theXCMmailrClient;

    static
    {
        // set up the shared XCMailr client instance
        final XltProperties props = XltProperties.getInstance();
        final String xcmailrBaseUrl = props.getProperty("xcmailr.baseUrl");
        final String xcmailrApiToken = props.getProperty("xcmailr.apiToken");

        XCMailrClient client = null;
        try
        {
            client = new XCMailrClient(xcmailrBaseUrl, xcmailrApiToken);
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
        theXCMmailrClient = client;
    }

    /**
     * Returns a JavaMail {@link Session} instance that is preconfigured with settings from the configuration.
     * 
     * @return the session instance
     */
    public static Session getMailSession()
    {
        // get test data
        final XltProperties props = XltProperties.getInstance();

        final String host = props.getProperty("smtp.host");
        final String port = props.getProperty("smtp.port");
        final String debug = props.getProperty("smtp.debug");
        final String requireStartTls = props.getProperty("smtp.requireStartTls");

        // create some properties
        final Properties smtpProps = new Properties();
        smtpProps.put("mail.smtp.host", host);
        smtpProps.put("mail.smtp.port", port);
        smtpProps.put("mail.debug", debug);

        // set some non-obvious properties to avoid DNS look-ups
        smtpProps.put("mail.host", host);
        smtpProps.put("mail.smtp.localhost", "localhost");

        // additional settings for STARTTLS
        smtpProps.put("mail.smtp.starttls.enable", requireStartTls);
        smtpProps.put("mail.smtp.starttls.required", requireStartTls);
        smtpProps.put("mail.smtp.ssl.trust", "*");

        // create and return the session
        return Session.getInstance(smtpProps, null);
    }

    /**
     * Returns the shared XCMailr client instance. It will be preconfigured with settings from the configuration.
     * 
     * @return the client instance
     */
    public static XCMailrClient getXCMailrClient()
    {
        return theXCMmailrClient;
    }

    /**
     * Executes the given action and logs a corresponding action data record to the results.
     * 
     * @param actionName
     *            the name of the action
     * @param action
     *            the action to be performed
     * @return the result of the action, or <code>null</code> if the action does not provide one
     * @throws Throwable
     *             any throwable thrown by the action
     */
    public static <T> T executeAction(final String actionName, final Action<T> action) throws Throwable
    {
        final ActionData actionData = new ActionData(actionName);

        final long start = TimerUtils.get().getStartTime();
        
        try
        {
            actionData.setTime(GlobalClock.millis());

            return action.run();
        }
        catch (final Throwable t)
        {
            actionData.setFailed(true);

            throw t;
        }
        finally
        {
            final long elapsed = TimerUtils.get().getElapsedTime(start);

            actionData.setRunTime(elapsed);

            SessionImpl.getCurrent().getDataManager().logDataRecord(actionData);
        }
    }
}
