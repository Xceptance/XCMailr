/**  
 *  Copyright 2013 the original author or authors.
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
 *
 */
package conf;

import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import ninja.utils.NinjaProperties;

/**
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
 */
@Singleton
public class XCMailrConf
{
    /**
     * specified with mbox.adminaddr
     */
    public final String ADMIN_ADDRESS;

    /**
     * specified with admin.pass
     */
    public final String ADMIN_PASSWORD;

    /**
     * specified with application.basedir
     */
    public final String APP_BASEPATH;

    /**
     * specified with application.url
     */
    public final String APP_HOME;

    /**
     * specified with application.languages
     */
    public final String[] APP_LANGS;

    /**
     * specified with application.name
     */
    public final String APP_NAME;

    /**
     * specified with application.default.entriesperpage default value is 15
     */
    public final Integer APP_DEFAULT_ENTRYNO;

    /**
     * indicates the use of whitelisting for registration
     */
    public final Boolean APP_WHITELIST;

    /**
     * specified with application.session.expire_time_in_seconds
     */
    public final Integer COOKIE_EXPIRETIME;

    /**
     * specified with application.cookie.prefix
     */
    public final String COOKIE_PREFIX;

    /**
     * specified with confirm.period
     */
    public final Integer CONFIRMATION_PERIOD;

    /**
     * specified with mbox.dlist (string-array)
     */
    public final String[] DOMAIN_LIST;

    /**
     * specified with mbox.host
     */
    public final String MB_HOST;

    /**
     * specified with mbox.interval
     */
    public final Integer MB_INTERVAL;

    /**
     * specified with mbox.port
     */
    public final Integer MB_PORT;

    /**
     * Whether or not the inbound SMTP server supports upgrading the connection to TLS.
     */
    public final Boolean MB_ENABLE_TLS;

    /**
     * Whether or not the inbound SMTP server requires the client to upgrade the connection to TLS. Effective only when
     * {@link #MB_ENABLE_TLS} is <code>true</code>.
     */
    public final Boolean MB_REQUIRE_TLS;

    /**
     * specified with memcached.host
     */
    public final String MEMCA_HOST;

    /**
     * indicates whether the forward-message should be wrapped in a new mail containing the original-message header in
     * the body
     */
    public final Boolean MSG_REWRITE;

    /**
     * The number of MTXs as limit to display at the mtx-page specified with mailtransaction.displaylimit. Default value
     * is 0 (no limit)
     */
    public final Integer MTX_LIMIT;

    /**
     * The number of MTXs as limit to display at the mtx-page specified with mailtransaction.maxage. Default value is -1
     * (no automatic deletion)
     */
    public final Integer MTX_MAX_AGE;

    /**
     * specified with mail.smtp.auth
     */
    public final Boolean OUT_SMTP_AUTH;

    /**
     * specified with mail.smtp.debug default value is true
     */
    public final Boolean OUT_SMTP_DEBUG;

    /**
     * specified with mail.smtp.host
     */
    public final String OUT_SMTP_HOST;

    /**
     * specified with mail.smtp.pass
     */
    public final String OUT_SMTP_PASS;

    /**
     * specified with mail.smtp.port
     */
    public final Integer OUT_SMTP_PORT;

    /**
     * specified with mail.smtp.tls
     */
    public final Boolean OUT_SMTP_TLS;

    /**
     * specified with mail.smtp.starttls
     */
    public final Boolean OUT_SMTP_STARTTLS;

    /**
     * specified with mail.smtp.user
     */
    public final String OUT_SMTP_USER;

    /**
     * specified with pw.length
     */
    public final Integer PW_LENGTH;

    /**
     * this is the cookie expiration-time in seconds as string suffixed with "s" for ninjaCache
     */
    public final String SESSION_EXPIRETIME;

    /**
     * Maximum size of a single mail that will be handled. Mails that exceed that size will be dropped.
     */
    public final int MAX_MAIL_SIZE;

    /**
     * The maximum time a temporary mail can be valid
     */
    public final int TEMPORARY_MAIL_MAX_VALID_TIME;

    /**
     * The amount of days an API token should be valid
     */
    public final int APITOKEN_EXPIRATION;

    /**
     * The amount of minutes an email should be preserved before being deleted. NOTE: this only accounts for emails that
     * were sent to an user defined email address that also has to be active
     */
    public final int MAIL_RETENTION_PERIOD;

    /**
     * The maximum number of days (including today) for which XCMailr maintains drop/forward mail statistics. Not
     * configurable.
     */
    public final int MAIL_STATISTICS_MAX_DAYS = 7;

    @Inject
    public XCMailrConf(NinjaProperties ninjaProp)
    {
        APP_NAME = ninjaProp.getOrDie("application.name");
        APP_HOME = ninjaProp.getOrDie("application.url");
        APP_BASEPATH = ninjaProp.getOrDie("application.basedir");
        APP_LANGS = filterDuplicates(ninjaProp.getStringArray("application.languages"), false);
        APP_DEFAULT_ENTRYNO = ninjaProp.getIntegerWithDefault("application.default.entriesperpage", 15);
        APP_WHITELIST = ninjaProp.getBooleanOrDie("application.whitelist");
        ADMIN_ADDRESS = ninjaProp.getOrDie("mbox.adminaddr");
        ADMIN_PASSWORD = ninjaProp.getOrDie("admin.pass");
        CONFIRMATION_PERIOD = ninjaProp.getIntegerOrDie("confirm.period");
        COOKIE_PREFIX = ninjaProp.getOrDie("application.cookie.prefix");
        COOKIE_EXPIRETIME = ninjaProp.getIntegerOrDie("application.session.expire_time_in_seconds");

        DOMAIN_LIST = filterDuplicates(ninjaProp.getStringArray("mbox.dlist"), true);
        MB_PORT = ninjaProp.getIntegerOrDie("mbox.port");
        MB_HOST = ninjaProp.getOrDie("mbox.host");
        MB_ENABLE_TLS = ninjaProp.getBooleanWithDefault("mbox.enableTls", true);
        MB_REQUIRE_TLS = MB_ENABLE_TLS && ninjaProp.getBooleanWithDefault("mbox.requireTls", false);
        MB_INTERVAL = ninjaProp.getIntegerOrDie("mbox.interval");

        MTX_LIMIT = ninjaProp.getIntegerWithDefault("mailtransaction.displaylimit", 0);
        MTX_MAX_AGE = ninjaProp.getIntegerWithDefault("mailtransaction.maxage", -1);
        MEMCA_HOST = ninjaProp.getOrDie("memcached.host");
        MSG_REWRITE = ninjaProp.getBooleanWithDefault("mail.msg.rewrite", false);

        OUT_SMTP_HOST = ninjaProp.getOrDie("mail.smtp.host");
        OUT_SMTP_PORT = ninjaProp.getIntegerOrDie("mail.smtp.port");
        OUT_SMTP_AUTH = ninjaProp.getBooleanOrDie("mail.smtp.auth");
        if (OUT_SMTP_AUTH)
        {
            OUT_SMTP_USER = ninjaProp.getOrDie("mail.smtp.user");
            OUT_SMTP_PASS = ninjaProp.getOrDie("mail.smtp.pass");
        }
        else
        {
            OUT_SMTP_USER = null;
            OUT_SMTP_PASS = null;
        }
        OUT_SMTP_TLS = ninjaProp.getBooleanOrDie("mail.smtp.tls");
        OUT_SMTP_STARTTLS = ninjaProp.getBooleanWithDefault("mail.smtp.starttls", false);
        OUT_SMTP_DEBUG = ninjaProp.getBooleanWithDefault("mail.smtp.debug", false);

        PW_LENGTH = ninjaProp.getIntegerOrDie("pw.length");
        SESSION_EXPIRETIME = COOKIE_EXPIRETIME + "s";
        MAX_MAIL_SIZE = ninjaProp.getIntegerOrDie("mbox.mail.maxsize");
        MAIL_RETENTION_PERIOD = ninjaProp.getIntegerOrDie("mbox.mail.retentionperiod");
        TEMPORARY_MAIL_MAX_VALID_TIME = ninjaProp.getIntegerOrDie("application.temporarymail.maximumvalidtime");

        APITOKEN_EXPIRATION = ninjaProp.getIntegerOrDie("application.api.tokenexpirationtime");

        /*
         * Verify that required settings are valid.
         */

        if (DOMAIN_LIST == null)
        {
            throw new RuntimeException("Key mbox.dlist does not exist. Please include it in your application.conf. "
                                       + "Otherwise this app will not work");
        }

        if (APP_LANGS == null)
        {
            throw new RuntimeException("Key 'application.languages' does not exist. Please include it in your application.conf. "
                                       + "Otherwise this app will not work");
        }
        if (APP_LANGS.length == 0)
        {
            throw new RuntimeException("Key 'application.languages' is empty. Please check your application.conf. "
                                       + "Otherwise this app will not work");
        }
    }

    private String[] filterDuplicates(final String[] args, final boolean ignoreCase)
    {
        if (args == null)
        {
            return null;
        }

        final ArrayList<String> list = new ArrayList<>();
        for (final String arg : args)
        {
            if (arg == null || list.stream().anyMatch(e -> ignoreCase ? StringUtils.equalsIgnoreCase(e, arg) : StringUtils.equals(e, arg)))
            {
                continue;
            }

            list.add(arg);
        }

        return list.toArray(new String[list.size()]);
    }
}
