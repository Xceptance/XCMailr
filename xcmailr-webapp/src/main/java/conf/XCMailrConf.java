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

import java.util.HashMap;
import java.util.Map;
import ninja.utils.NinjaProperties;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
 */
@Singleton
public class XCMailrConf
{
    public final String APP_NAME;
    
    public final String APP_HOME;
    
    public final String APP_BASE;

    public final String C_PREFIX;

    public final Integer MB_PORT;

    public final String MB_HOST;

    public final Integer MB_INT;

    public final String D_LIST;

    public final String[] DM_LIST;

    public final String ADMIN_ADD;

    public final String ADMIN_PASS;

    public final Integer MAIL_INT;

    public final Integer MB_SIZE;

    public final Integer PW_LEN;

    public final Integer CONF_PERIOD;

    public final String OUT_SMTP_HOST;

    public final Integer OUT_SMTP_PORT;

    public final String OUT_SMTP_USER;

    public final String OUT_SMTP_PASS;

    public final Boolean OUT_SMTP_AUTH;

    public final Boolean OUT_SMTP_TLS;

    public final String MC_HOST;

    public final Integer MC_PORT;


    @Inject
    public XCMailrConf(NinjaProperties ninjaProp)
    {
        APP_NAME = ninjaProp.getOrDie("application.name");
        APP_HOME = ninjaProp.getOrDie("application.url");
        APP_BASE = ninjaProp.getOrDie("application.basedir");
        C_PREFIX = ninjaProp.getOrDie("application.cookie.prefix");
        MB_PORT = ninjaProp.getIntegerOrDie("mbox.port");
        MB_HOST = ninjaProp.getOrDie("mbox.host");
        MB_INT = ninjaProp.getIntegerOrDie("mbox.interval");
        D_LIST = ninjaProp.getOrDie("mbox.dlist");
        DM_LIST = ninjaProp.getStringArray("mbox.dlist");
        ADMIN_ADD = ninjaProp.getOrDie("mbox.adminaddr");
        ADMIN_PASS = ninjaProp.getOrDie("admin.pass");
        MAIL_INT = ninjaProp.getIntegerOrDie("mbox.mailinterval");
        MB_SIZE = ninjaProp.getIntegerOrDie("mbox.size");
        PW_LEN = ninjaProp.getIntegerOrDie("pw.length");
        CONF_PERIOD = ninjaProp.getIntegerOrDie("confirm.period");
        OUT_SMTP_HOST = ninjaProp.getOrDie("mail.smtp.host");
        OUT_SMTP_PORT = ninjaProp.getIntegerOrDie("mail.smtp.port");
        OUT_SMTP_USER = ninjaProp.getOrDie("mail.smtp.user");
        OUT_SMTP_PASS = ninjaProp.getOrDie("mail.smtp.pass");
        OUT_SMTP_AUTH = ninjaProp.getBooleanOrDie("mail.smtp.auth");
        OUT_SMTP_TLS = ninjaProp.getBooleanOrDie("mail.smtp.tls");
        MC_HOST = ninjaProp.getOrDie("memcached.host");
        MC_PORT = ninjaProp.getIntegerOrDie("memcached.port");


        if (DM_LIST == null)
        {
            throw new RuntimeException("Key mbox.dlist does not exist. Please include it in your application.conf. "
                                       + "Otherwise this app will not work");
        }
    }

    /**
     * Extracts the List of Domains and returns it as a nicely renderable Map
     * 
     * @return a Map with a "domain"-key which contains the list of domains
     */
    public Map<String, Object> getDomListAsMap()
    {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("domain", DM_LIST);
        return map;
    }
}
