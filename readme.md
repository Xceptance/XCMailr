# XCMailr
## Summary
* Name: XCMailr
* Version: 1.1.5
* Release: Mar. 2015
* License: Apache V2.0
* License URI: http://www.apache.org/licenses/LICENSE-2.0.txt
* Tags: AntiSpam, Testutility
* Contributors:
 * Patrick Thum, Xceptance Software Technologies GmbH
 * Patrick Hähnlein, Xceptance Software Technologies GmbH

## Description
XCMailr has been built to aid testing. Testing needs email addresses to create accounts, test formats of emails addresses, and last but not least load testing needs a ton of that. A special challenge for load testing is, that the mails should be deliverable, so that the sending system is not overwhelmed by returns. 

Commercial or free system do not want to see 20k fake accounts that receive about 1,500 emails per day each. Your sender will be blocked quickly. Additionally you often cannot disable email delivery because the system under test does either not have that option or it is out of reach.

XCMailr lets you quickly and easily setup email accounts for testing purposes. Simply create temporary email addresses and have all incoming email forwarded to your actual account. When its configurable lifetime expires, the address gets deactivated and all incoming mail will be silently dropped. XCMailr doesn't store any email content and and won't notify the sender if the address is no longer valid.

So you host XCMailr yourself, you control security, you control availability, you control the domains, you control everything. Hence XCMailr is perfect for real-life testing of sensitive systems as well, because no third party will see your test emails and draw conclusion from it.

## Requirements
* [Memcached](http://memcached.org/), optional since V1.1.3 but recommended

## Configuration
* Open and edit the application.conf in conf/ . 
* It is strongly recommended to create a new application secret. This secret ensures that the session-cookie of a user has not been modified. 
* You should especially customize the following settings:
 * **application.secret** uncomment and set this line, its used to verify session-cookies
 * **mbox.dlist** the list of available domains
 * **mbox.host** the main-application-host 
 * **mail.smtp.&#42;** the "outbound" SMTP-Server (the server to which the application will forward any "valid" messages)
 * **memcached.&#42;** the MemCached-Server (host and port)
 * **ebean.&#42;** the Ebean-Configuration
* You can configure the logging by editing conf/logback.xml. See http://logback.qos.ch/manual/ for documentation.
* HTTPS support, Edit conf/jetty.xml-file as shown here: http://blog.callistaenterprise.se/2011/11/24/quick-start-jettys-maven-plugin-with-ssl/

## Run the Application
* Just rename the application.conf.template to application.conf and edit the application.conf (see Configuration). Then run the 'run.sh'-script.
* If you set a value for "application.basedir", the server will use that value as contextpath for your application. That means when you specify the value "xcmailr" for the basedir and "http://localhost:8080" as "application.url", then your application can be locally reached at "http://localhost:8080/xcmailr". If you want to run the application behind a reverse proxy, have a look at the section below.
* To drop and recreate all tables (which will remove all data contained in this tables!), run the script with the parameter "-Dxcmailr.xcmstart.droptables=true".

## Build from Source
* If you want to build the project from the sources, you have two options to run the webapp.
* First option (after you've changed something and want to check your changes), the development-mode:
 * cd into the 'xcmailr-webapp' folder
 * execute 'mvn clean jetty:run' to clean up the target-folder (if existent) and run the app in development-mode inside an embedded-jetty running on localhost:8080 (the "basedir" will be ignored here)
 * NOTE: The webapp does not check whether the database and the tables exist. On the first run, you must set "%dev.ebean.ddl.run" and "%dev.ebean.ddl.generate" in application.conf to "true" to execute the "create table"-scripts and run the app successfully (afterwards you should set these values to "false" again, otherwise the database will be dropped and recreated after each server-reload).
 * NOTE2 (especially for contributors): You probably want to change the configuration-file in dev-mode. Thereby, you should either set a gitignore (or svn:ignore) to prevent that your personal data (e.g. the mailservice-login) will be committed to the repository or you can place another application.conf at /home/yourUsername/conf/ . The ninja-framework uses Apache Commons Configuration to read the file. It will search for the configuration-file at first in this folder. In both cases you have to take care that the .conf-files at ./xcmailr-webapp/src/main/java/conf and ./xcmailr-resources/conf are up-to-date and contain all necessary keys.
* Second option (to create the build-folder):
 * cd into the home-directory of XCMailr
 * run 'mvn clean package' to create the build-folder
 * now there will be a folder called 'xcmailr-build' which contains the known files and can be executed as explained in the section "Run the Application"

## Using an Apache Reverse-Proxy
* You may want to use an (Apache2) reverse-proxy in front of the application. With the "application.basedir"-option in the application.conf, you can specify a context-path for your application. Thereby, you can use the same path on which the app will be available through the proxy. 
* For instance, if it will be available externally at "http://reverse.proxy/path/to/app", then you can set the basedir to "path/to/app" and it will run locally at "http://localhost:port/path/to/app".
* The advantage is that you don't have to use the mod_proxy_html-module to rewrite every link and file-paths on all html-pages.
* After setting up the Apache2 with all necessary Proxy-Modules (especially you have to enable proxy and proxy_http), you'll have to create a VirtualHost-Configuration for your site. Here's a small example for that:

```apache
<VirtualHost *:80>  
    ProxyRequests off  
    ProxyPass /xcmailr/ http://localhost:8080/xcmailr/  
    ProxyPassReverse /xcmailr/ http://localhost:8080/xcmailr/  
        
    <Proxy http://localhost:8080*>  
        Order deny,allow  
        allow from all  
    </Proxy>  
       
    Redirect /xcmailr       /xcmailr/  
</VirtualHost>  
```

* We set the basedir to "xcmailr" and our app then runs locally on "http://localhost:8080/xcmailr", externally it can be reached with this configuration at http://mydomain/xcmailr. 


## 

## API Token
XCMailr supports API token to access some XCMailr functionality without using e-mail and password to authenticate. An API token can be genererated in the edit profile menu.
[token](images/API_Token.png)

You can create a new token or revoke an earlier generated token.

## API
An http based API was added to XCMailr containing the following functionality.

### Create a temporary email address
Create a new temporary email address that will be associated with you already registered account. That temporary email address has a limited life span which can be defined with parameter validTime. The parameter is a natural number indicating how many minutes that temporary email will be active. The upper limit for the maximum allowed time span can be configured in application.conf (application.temporarymail.maximumvalidtime, default value is 30).

The parameter mailAddress is the full address that is desired to claim. E.g. foo@bar.com provided that you configured XCMailr to serve email for bar.com
The parameter token is the token that can be created in thee edit profile menu. 
   
http://xcmailrhost/create/temporaryMail/{token}/{mailAddress}/{validTime}

e.g. http://xcmailrhost/create/temporaryMail/MyAccessToken/foo@bar.com/5
Uses the token (here "MyAccessToken" for) to claim address foo@bar.com. The mail address can receive emails for 5 minutes before the address expires. An expired address can be reactivated by using the same call given that your account is associated with the desired address. In case another account claimed that address before a http error is thrown.  

In case of any error during temporary mail creation an http error is thrown, no further advice is given.

### Access mailbox
XCMailr behavior has changed recently. Emails sent to an active mail address will now be saved for a limited time (10 minutes, this can't be changed at the moment). The received emails can be accessed through the web interface. See "My Emails" once you are logged in.
  
There is also an API functionality which allows to also filter received emails for a given mail address. In order to do so one can use the following URL and the following parameter.
mailAddress is the full address that is claimed by the used account. The parameter token can be generated in the edit profile dialog 
http://xcmailrhost/mailbox/{mailAddress}/{token} 

Url parameter

from: a regular expression to find in the address the mail was sent from
subject: a regular expression to find in the emails subject
textContent: a regular expression to find in the emails text content
htmlContent: a regular expression to find in the emails html content
plainMail: a regular expression to find in the plain mails
lastMatch: a parameter without value that limits the result set to one entry. This is the last filter that will be applied to result set.
format: a string indicating the desired response format. If not defined then the result will be displayed as html. Valid values are "json" and "plain". With format json the results will be returned as json formatted string. The format plain is used to retrieve the mail in the format the mail server received it. This contains also all email header and encoding fields. Also the plain format will automatically limit the results to one entry since multiple results could hardly distinguished in the repsonse.

Note: plain mail filter will be used on the mails raw byte stream that is stored on receive.

http://xcmailrhost/mailbox/foo@bar.com/MyAccessToken?subject=


## Frameworks/Librarys/Code/etc. Provided by Others
### AngularJS
* http://angularjs.org
* MIT-License: http://github.com/angular/angular.js/blob/master/LICENSE

### AngularUI-Bootstrap
* https://github.com/angular-ui/bootstrap
* MIT-License: https://github.com/angular-ui/bootstrap/blob/master/LICENSE

### Avaje Ebean 
* http://www.avaje.org/
* LGPL: http://www.gnu.org/licenses/lgpl.html

### Bootstrap Datetimepicker
* https://github.com/Eonasdan/bootstrap-datetimepicker
* MIT-License: https://github.com/Eonasdan/bootstrap-datetimepicker/blob/master/LICENSE

### H2 Database Engine
* http://www.h2database.com/
* Dual licensed
 * H2 License, V1.0: http://www.h2database.com/html/license.html#h2_license
 * EPL: http://www.h2database.com/html/license.html#eclipse_license

### Icons by Glyphicons (shipped with Twitter Bootstrap)
* Copyright Jan Kovařík
* http://glyphicons.com/
* Apache V2.0 License: https://github.com/twitter/bootstrap/wiki/License

### JBCrypt - a Java BCrypt implementation 
* Copyright (c) 2006 Damien Miller
* http://www.mindrot.org/projects/jBCrypt/
* ISC/BSD License: http://www.mindrot.org/files/jBCrypt/LICENSE

### Jetty 9
* http://www.eclipse.org/jetty/
* Apache V2.0 License: http://www.apache.org/licenses/LICENSE-2.0

### JodaTime
* http://joda-time.sourceforge.net
* Apache V2.0 License: http://joda-time.sourceforge.net/license.html

### jQuery Tablesorter 2.0 plugin
* http://tablesorter.com
* Dual licensed 
 * MIT-License: http://www.opensource.org/licenses/mit-license.php
 * GPL: http://www.opensource.org/licenses/gpl-license.php

### Moment
* https://github.com/moment/moment/ 
* MIT-License: https://github.com/moment/moment/blob/develop/LICENSE

### NinjaFramework
* http://www.ninjaframework.org/
* Apache V2.0 License: https://github.com/reyez/ninja/blob/develop/license.txt

### Spymemcached
* http://code.google.com/p/spymemcached/
* MIT-License: http://www.opensource.org/licenses/mit-license.php

### Twitter Bootstrap
* https://github.com/twbs/bootstrap
* MIT-License: https://github.com/twbs/bootstrap/blob/master/LICENSE

### Twitter Bloodhound (as part of Typeahead)
* https://github.com/twitter/typeahead.js
* MIT-License: https://github.com/twitter/typeahead.js/blob/master/LICENSE

## License
XCMailr is licensed under the Apache Version 2.0 license.
See LICENSE file for full license text.
