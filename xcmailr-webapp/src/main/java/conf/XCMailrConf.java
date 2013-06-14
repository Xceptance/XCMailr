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

import ninja.utils.NinjaProperties;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
 */
@Singleton
public class XCMailrConf
{
    public final String ADMIN_ADDRESS;

    public final String ADMIN_PASSWORD;

    public final String APP_BASEPATH;

    public final String APP_HOME;

    public final String[] APP_LANGS;

    public final String APP_NAME;
    
    public final Integer APP_DEFAULT_ENTRYNO;

    public final Integer COOKIE_EXPIRETIME;

    public final String COOKIE_PREFIX;

    public final Integer CONFIRMATION_PERIOD;

    public final String D_LIST;

    public final String[] DOMAIN_LIST;

    public final String MB_HOST;

    public final Integer MB_INTERVAL;

    public final Integer MB_PORT;

    public final String MEMCA_HOST;

    public final Integer MEMCA_PORT;

    public final Boolean OUT_SMTP_AUTH;

    public final Boolean OUT_SMTP_DEBUG;

    public final String OUT_SMTP_HOST;

    public final String OUT_SMTP_PASS;

    public final Integer OUT_SMTP_PORT;

    public final Boolean OUT_SMTP_TLS;

    public final String OUT_SMTP_USER;

    public final Integer PW_LENGTH;

    @Inject
    public XCMailrConf(NinjaProperties ninjaProp)
    {
        APP_NAME = ninjaProp.getOrDie("application.name");
        APP_HOME = ninjaProp.getOrDie("application.url");
        APP_BASEPATH = ninjaProp.getOrDie("application.basedir");
        APP_LANGS = ninjaProp.getStringArray("application.languages");
        APP_DEFAULT_ENTRYNO = ninjaProp.getIntegerWithDefault("application.default.entriesperpage", 15);
        ADMIN_ADDRESS = ninjaProp.getOrDie("mbox.adminaddr");
        ADMIN_PASSWORD = ninjaProp.getOrDie("admin.pass");
        COOKIE_PREFIX = ninjaProp.getOrDie("application.cookie.prefix");
        COOKIE_EXPIRETIME = ninjaProp.getIntegerOrDie("application.session.expire_time_in_seconds");
        MB_PORT = ninjaProp.getIntegerOrDie("mbox.port");
        MB_HOST = ninjaProp.getOrDie("mbox.host");
        MB_INTERVAL = ninjaProp.getIntegerOrDie("mbox.interval");
        D_LIST = ninjaProp.getOrDie("mbox.dlist");
        DOMAIN_LIST = ninjaProp.getStringArray("mbox.dlist");
        PW_LENGTH = ninjaProp.getIntegerOrDie("pw.length");
        CONFIRMATION_PERIOD = ninjaProp.getIntegerOrDie("confirm.period");
        OUT_SMTP_HOST = ninjaProp.getOrDie("mail.smtp.host");
        OUT_SMTP_PORT = ninjaProp.getIntegerOrDie("mail.smtp.port");
        OUT_SMTP_USER = ninjaProp.getOrDie("mail.smtp.user");
        OUT_SMTP_PASS = ninjaProp.getOrDie("mail.smtp.pass");
        OUT_SMTP_AUTH = ninjaProp.getBooleanOrDie("mail.smtp.auth");
        OUT_SMTP_TLS = ninjaProp.getBooleanOrDie("mail.smtp.tls");
        OUT_SMTP_DEBUG = ninjaProp.getBooleanWithDefault("mail.smtp.debug", true);
        MEMCA_HOST = ninjaProp.getOrDie("memcached.host");
        MEMCA_PORT = ninjaProp.getIntegerOrDie("memcached.port");

        if (DOMAIN_LIST == null)
        {
            throw new RuntimeException("Key mbox.dlist does not exist. Please include it in your application.conf. "
                                       + "Otherwise this app will not work");
        }
    }
}
