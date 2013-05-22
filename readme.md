XCMailr
=======
Summary
------
* Name: XCMailr
* Version: 1.0
* Release: May 2013
* License: Apache V2.0
* License URI: http://www.apache.org/licenses/LICENSE-2.0.txt
* Tags: AntiSpam, Testutility
* Contributors:
    * Patrick Thum, Xceptance Software Technologies GmbH
    *  

Description:
------------
XCMailr helps you to reduce the amount of spam in your personal E-Mail-Account. Create temporary Mailaddresses which will be automatically forwarded to your own Mail. The Mail-Forwards will expire automatically after a given validity period or at every time you want to. XCMailr will not save any contents of your mails.


Requirements
-------------
* Maven
* H2 Database (or any other Database that is supported by Avaje Ebean, please take a look at avaje.org/ebean for further Information)
* Memcached ( http://memcached.org/ )


Configuration:
--------------
* Open and edit the application.conf in conf/ 
* It is strongly recommended to create a new application secret. This secret ensures that the Session-Cookie of a User has not been modified. 
* You should especially customize the following settings:
    * **mbox.dlist**: the list of available domains
    * **mbox.host** the main-application-host 
    * ** mail.smtp.* ** the "outbound" SMTP-Server (the Server to which the Application will forward any Messages )
    * ** memcached.* ** the MemCached-Server (host and port)
    * ** ebean.* ** the Ebean-Configuration
* To run the Application in Production-Mode, you have to set the Java system property **ninja.mode** with the value "prod". This can be reached for example by setting the MAVEN_OPTS environment variable:
    * `MAVEN_OPTS="-ninja.mode=prod"`
    * `export $MAVEN_OPTS`
    * You may also add additional Java-Parameters like -Xmx, -Xms for maximal and initial heap spaces or -XX:MaxPermSize for maximal permanent space (have a look at the documentation of your JVM for further informations)


* HTTPS Support: http://blog.callistaenterprise.se/2011/11/24/quick-start-jettys-maven-plugin-with-ssl/

Frameworks/Librarys/Code/etc which were provided by others:
-----------------------------------------------------------
* Avaje Ebean 
    * http://www.avaje.org/
    * LGPL: http://www.gnu.org/licenses/lgpl.html

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

* Icons by Glyphicons (shipped with Twitter Bootstrap)
    * Copyright Jan Kovařík
    * http://glyphicons.com/
    * Apache V2.0 License: https://github.com/twitter/bootstrap/wiki/License
  	
* jQuery Tablesorter 2.0 plugin
    * 
    * http://tablesorter.com
    * Dual licensed 
         * MIT-License: http://www.opensource.org/licenses/mit-license.php
         * GPL-License: http://www.opensource.org/licenses/gpl-license.php

License:
--------
XCMailr is licensed under the Apache Version 2.0 license.
See LICENSE file for full license text.
