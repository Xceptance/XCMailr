/*
 * Copyright (c) 2013-2026 Xceptance Software Technologies GmbH
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
package com.xceptance.xcmailr.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly typed configuration properties for the XCMailr application.
 * Mapped from the {@code xcmailr} prefix in {@code application.yml}.
 *
 * @author Xceptance Software Technologies GmbH
 */
@ConfigurationProperties(prefix = "xcmailr")
public class XcmailrProperties
{
    private AppProperties app = new AppProperties();
    private AdminProperties admin = new AdminProperties();
    private ApiProperties api = new ApiProperties();
    private TemporaryMailProperties temporaryMail = new TemporaryMailProperties();
    private MboxProperties mbox = new MboxProperties();
    private MailTransactionProperties mailTransaction = new MailTransactionProperties();
    private OutboundSmtpProperties outboundSmtp = new OutboundSmtpProperties();

    public AppProperties getApp()
    {
        return app;
    }

    public void setApp(final AppProperties app)
    {
        this.app = app;
    }

    public AdminProperties getAdmin()
    {
        return admin;
    }

    public void setAdmin(final AdminProperties admin)
    {
        this.admin = admin;
    }

    public ApiProperties getApi()
    {
        return api;
    }

    public void setApi(final ApiProperties api)
    {
        this.api = api;
    }

    public TemporaryMailProperties getTemporaryMail()
    {
        return temporaryMail;
    }

    public void setTemporaryMail(final TemporaryMailProperties temporaryMail)
    {
        this.temporaryMail = temporaryMail;
    }

    public MboxProperties getMbox()
    {
        return mbox;
    }

    public void setMbox(final MboxProperties mbox)
    {
        this.mbox = mbox;
    }

    public MailTransactionProperties getMailTransaction()
    {
        return mailTransaction;
    }

    public void setMailTransaction(final MailTransactionProperties mailTransaction)
    {
        this.mailTransaction = mailTransaction;
    }

    public OutboundSmtpProperties getOutboundSmtp()
    {
        return outboundSmtp;
    }

    public void setOutboundSmtp(final OutboundSmtpProperties outboundSmtp)
    {
        this.outboundSmtp = outboundSmtp;
    }

    /**
     * General application configuration properties.
     */
    public static class AppProperties
    {
        private String name = "XCMailr";
        private String url = "http://localhost:8080";
        private List<String> languages = List.of("en", "de");
        private int defaultEntriesPerPage = 15;
        private boolean whitelist = true;
        private int confirmationPeriodHours = 1;
        private int cookieExpireTimeSeconds = 3600;
        private String cookiePrefix = "XCMailr";

        public String getName()
        {
            return name;
        }

        public void setName(final String name)
        {
            this.name = name;
        }

        public String getUrl()
        {
            return url;
        }

        public void setUrl(final String url)
        {
            this.url = url;
        }

        public List<String> getLanguages()
        {
            return languages;
        }

        public void setLanguages(final List<String> languages)
        {
            this.languages = languages;
        }

        public int getDefaultEntriesPerPage()
        {
            return defaultEntriesPerPage;
        }

        public void setDefaultEntriesPerPage(final int defaultEntriesPerPage)
        {
            this.defaultEntriesPerPage = defaultEntriesPerPage;
        }

        public boolean isWhitelist()
        {
            return whitelist;
        }

        public void setWhitelist(final boolean whitelist)
        {
            this.whitelist = whitelist;
        }

        public int getConfirmationPeriodHours()
        {
            return confirmationPeriodHours;
        }

        public void setConfirmationPeriodHours(final int confirmationPeriodHours)
        {
            this.confirmationPeriodHours = confirmationPeriodHours;
        }

        public int getCookieExpireTimeSeconds()
        {
            return cookieExpireTimeSeconds;
        }

        public void setCookieExpireTimeSeconds(final int cookieExpireTimeSeconds)
        {
            this.cookieExpireTimeSeconds = cookieExpireTimeSeconds;
        }

        public String getCookiePrefix()
        {
            return cookiePrefix;
        }

        public void setCookiePrefix(final String cookiePrefix)
        {
            this.cookiePrefix = cookiePrefix;
        }
    }

    /**
     * Initial administrator account settings.
     */
    public static class AdminProperties
    {
        private String address = "admin@xcmailr.test";
        private String password = "1234";

        public String getAddress()
        {
            return address;
        }

        public void setAddress(final String address)
        {
            this.address = address;
        }

        public String getPassword()
        {
            return password;
        }

        public void setPassword(final String password)
        {
            this.password = password;
        }
    }

    /**
     * API token settings.
     */
    public static class ApiProperties
    {
        private int tokenExpirationDays = 30;

        public int getTokenExpirationDays()
        {
            return tokenExpirationDays;
        }

        public void setTokenExpirationDays(final int tokenExpirationDays)
        {
            this.tokenExpirationDays = tokenExpirationDays;
        }
    }

    /**
     * Temporary mail address generation settings.
     */
    public static class TemporaryMailProperties
    {
        private int maximumValidTimeMinutes = 30;

        public int getMaximumValidTimeMinutes()
        {
            return maximumValidTimeMinutes;
        }

        public void setMaximumValidTimeMinutes(final int maximumValidTimeMinutes)
        {
            this.maximumValidTimeMinutes = maximumValidTimeMinutes;
        }
    }

    /**
     * Inbound mailbox and SMTP settings.
     */
    public static class MboxProperties
    {
        private String host = "xcmailr.test";
        private int port = 25000;
        private boolean enableTls = true;
        private boolean requireTls = false;
        private int intervalMinutes = 1;
        private int mailIntervalMinutes = 1;
        private List<String> domainList = List.of("xcmailr.test", "ccmailr.test");
        private int maxSize = 25000000;
        private int retentionPeriodMinutes = 10;

        public String getHost()
        {
            return host;
        }

        public void setHost(final String host)
        {
            this.host = host;
        }

        public int getPort()
        {
            return port;
        }

        public void setPort(final int port)
        {
            this.port = port;
        }

        public boolean isEnableTls()
        {
            return enableTls;
        }

        public void setEnableTls(final boolean enableTls)
        {
            this.enableTls = enableTls;
        }

        public boolean isRequireTls()
        {
            return requireTls;
        }

        public void setRequireTls(final boolean requireTls)
        {
            this.requireTls = requireTls;
        }

        public int getIntervalMinutes()
        {
            return intervalMinutes;
        }

        public void setIntervalMinutes(final int intervalMinutes)
        {
            this.intervalMinutes = intervalMinutes;
        }

        public int getMailIntervalMinutes()
        {
            return mailIntervalMinutes;
        }

        public void setMailIntervalMinutes(final int mailIntervalMinutes)
        {
            this.mailIntervalMinutes = mailIntervalMinutes;
        }

        public List<String> getDomainList()
        {
            return domainList;
        }

        public void setDomainList(final List<String> domainList)
        {
            this.domainList = domainList;
        }

        public int getMaxSize()
        {
            return maxSize;
        }

        public void setMaxSize(final int maxSize)
        {
            this.maxSize = maxSize;
        }

        public int getRetentionPeriodMinutes()
        {
            return retentionPeriodMinutes;
        }

        public void setRetentionPeriodMinutes(final int retentionPeriodMinutes)
        {
            this.retentionPeriodMinutes = retentionPeriodMinutes;
        }
    }

    /**
     * Mail transaction audit logging properties.
     */
    public static class MailTransactionProperties
    {
        private int displayLimit = 9000;
        private int maxAgeHours = 168;

        public int getDisplayLimit()
        {
            return displayLimit;
        }

        public void setDisplayLimit(final int displayLimit)
        {
            this.displayLimit = displayLimit;
        }

        public int getMaxAgeHours()
        {
            return maxAgeHours;
        }

        public void setMaxAgeHours(final int maxAgeHours)
        {
            this.maxAgeHours = maxAgeHours;
        }
    }

    /**
     * Outbound SMTP relay configuration properties.
     */
    public static class OutboundSmtpProperties
    {
        private boolean enabled = true;
        private boolean ssl = false;
        private String host;
        private int port = 25;
        private String user;
        private String password;
        private boolean auth = true;
        private boolean tls = true;
        private boolean starttls = false;
        private boolean debug = true;
        private boolean msgRewrite = false;

        public boolean isEnabled()
        {
            return enabled;
        }

        public void setEnabled(final boolean enabled)
        {
            this.enabled = enabled;
        }

        public boolean isSsl()
        {
            return ssl;
        }

        public void setSsl(final boolean ssl)
        {
            this.ssl = ssl;
        }

        public String getHost()
        {
            return host;
        }

        public void setHost(final String host)
        {
            this.host = host;
        }

        public int getPort()
        {
            return port;
        }

        public void setPort(final int port)
        {
            this.port = port;
        }

        public String getUser()
        {
            return user;
        }

        public void setUser(final String user)
        {
            this.user = user;
        }

        public String getPassword()
        {
            return password;
        }

        public void setPassword(final String password)
        {
            this.password = password;
        }

        public boolean isAuth()
        {
            return auth;
        }

        public void setAuth(final boolean auth)
        {
            this.auth = auth;
        }

        public boolean isTls()
        {
            return tls;
        }

        public void setTls(final boolean tls)
        {
            this.tls = tls;
        }

        public boolean isStarttls()
        {
            return starttls;
        }

        public void setStarttls(final boolean starttls)
        {
            this.starttls = starttls;
        }

        public boolean isDebug()
        {
            return debug;
        }

        public void setDebug(final boolean debug)
        {
            this.debug = debug;
        }

        public boolean isMsgRewrite()
        {
            return msgRewrite;
        }

        public void setMsgRewrite(final boolean msgRewrite)
        {
            this.msgRewrite = msgRewrite;
        }
    }
}
