XCMailr
=======
Summary
------
 * Name: XCMailr
 * Version: 1.0.7
 * Release: June 2013
 * License: Apache V2.0
 * License URI: http://www.apache.org/licenses/LICENSE-2.0.txt
 * Tags: AntiSpam, Testutility
 * Contributors:
    * Patrick Thum, Xceptance Software Technologies GmbH
    * Patrick Hähnlein, Xceptance Software Technologies GmbH
    * 

Description:
------------
XCMailr helps you to reduce the amount of spam in your personal E-Mail-Account. Create temporary Mailaddresses which will be automatically forwarded to your own Mail. The Mail-Forwards will expire automatically after a given validity period or at every time you want to. XCMailr will not save any contents of your mails.


Requirements
-------------
 * Memcached ( http://memcached.org/ )


Configuration:
--------------
 * Open and edit the application.conf in conf/ 
 * It is strongly recommended to create a new application secret. This secret ensures that the Session-Cookie of a User has not been modified. 
 * You should especially customize the following settings:
    * **application.secret** uncomment and set this line, its used to verify session-cookies
    * **mbox.dlist**: the list of available domains
    * **mbox.host** the main-application-host 
    * ** mail.smtp.* ** the "outbound" SMTP-Server (the Server to which the Application will forward any "valid" Messages )
    * ** memcached.* ** the MemCached-Server (host and port)
    * ** ebean.* ** the Ebean-Configuration

 * To reach the Admin-Panel visit http://yourdomain/admin (a more comfortable solution will come in a later version).
 * You can configure the logging by editing conf/logback.xml, see http://logback.qos.ch/manual/ for documentation
 * HTTPS Support, Edit conf/jetty.xml-file as shown here: http://blog.callistaenterprise.se/2011/11/24/quick-start-jettys-maven-plugin-with-ssl/

Run the Application:
--------------------
 * Just edit the application.conf (see Configuration) and run the 'run.sh'-script
 * To drop and recreate all Tables (this will remove all data contained in this tables!) run the script with the parameter "-Dxcmailr.xcmstart.droptables=true"

Build from Source:
------------------
 * If you want to build the Project from the sources, you've two options to run the webapp.
 * First option (after you've changed sth. and want to check your changes), the development-mode:
    * cd into the 'xcmailr-webapp' folder
    * execute 'mvn clean jetty:run' to clean up the target-folder (if existent) and run the app in development-mode inside an embedded-jetty running on localhost:8080
    * NOTE1: The webapp does not check whether the database and the tables exist. On the first run, you must set "%dev.ebean.ddl.run" and "%dev.ebean.ddl.generate" in application.conf to "true" to execute the "create table"-scripts and run the app successfully. (after that, you should set these values to "false" again, otherwise the database will be dropped and recreated after each server-reload)
    * NOTE2 (especially for contributors): You probably want to change the configuration-file in dev-mode. Thereby you should either set a gitignore (or svn:ignore) to prevent that your personal data (e.g. the mailservice-login) will be committed to the repository or you can place another application.conf at /home/yourUsername/conf/ . The ninja-framework uses Apache Commons Configuration to read the file and this will search for the file at first in this folder. In both cases you have to care that the .conf-files at ./xcmailr-webapp/src/main/java/conf and ./xcmailr-resources/conf are up-to-date and contain all necessary keys
 * Second option (to create the build-folder):
    * cd into the home-directory of the xcmailr
    * run 'mvn clean package' to create the build-folder
    * after that, there will be a folder called 'xcmailr-build' which contains the known files and can be executed as explained in the section "run the application"



Frameworks/Librarys/Code/etc which were provided by others:
-----------------------------------------------------------
 * Avaje Ebean 
    * http://www.avaje.org/
    * LGPL: http://www.gnu.org/licenses/lgpl.html

 * Bootstrap Datetimepicker
    * http://tarruda.github.io/bootstrap-datetimepicker/
    * Apache V2.0 License: http://www.apache.org/licenses/LICENSE-2.0

 * H2 Database Engine
    * http://www.h2database.com/
    * Dual licensed
         * H2 License, V1.0: http://www.h2database.com/html/license.html#h2_license
         * EPL: http://www.h2database.com/html/license.html#eclipse_license

 * Icons by Glyphicons (shipped with Twitter Bootstrap)
    * Copyright Jan Kovařík
    * http://glyphicons.com/
    * Apache V2.0 License: https://github.com/twitter/bootstrap/wiki/License

 * Jetty 6
    * http://www.eclipse.org/jetty/
    * Apache V2.0 License: http://www.apache.org/licenses/LICENSE-2.0
  	
 * jQuery Tablesorter 2.0 plugin
    * http://tablesorter.com
    * Dual licensed 
         * MIT-License: http://www.opensource.org/licenses/mit-license.php
         * GPL: http://www.opensource.org/licenses/gpl-license.php

 * Spymemcached
    * http://code.google.com/p/spymemcached/
    * MIT-License: http://www.opensource.org/licenses/mit-license.php

 * JBCrypt - a Java BCrypt implementation 
    * Copyright (c) 2006 Damien Miller
    * http://www.mindrot.org/projects/jBCrypt/
    * ISC/BSD License: http://www.mindrot.org/files/jBCrypt/LICENSE

 * JodaTime
    * http://joda-time.sourceforge.net
    * Apache V2.0 License: http://joda-time.sourceforge.net/license.html

 * NinjaFramework
    * http://www.ninjaframework.org/
    * Apache V2.0 License: https://github.com/reyez/ninja/blob/develop/license.txt

 * Twitter Bootstrap
    * Copyright 2011 Twitter, Inc.
    * http://twitter.github.io/bootstrap/
    * Apache V2.0 License: https://github.com/twitter/bootstrap/wiki/License




License:
--------
XCMailr is licensed under the Apache Version 2.0 license.
See LICENSE file for full license text.
