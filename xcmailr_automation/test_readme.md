## Information and instructions about the XCMailr test automation suite


### Table of Contents
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1. Overview
2. Requirements
3. Getting Started
3.1. XLT Script Developer
3.2. WebDriver tests with ANT
4. Additional Help
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


### 1. Overview

The content of the 'xcmailr_automation' folder covers the UI test automation for the XCMailr application.

The test suite provides tests of the most functionalities on the XCMailr website such as creation and customization of temporary email addresses 
as well as user management like registration.
The most important part are the workflows where the possibility of email forwarding and suppression will be verified.

An explanation about the usage and requirements will be given in the following.


~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
### 2. Requirements

* Latest XLT release: http://www.xceptance.com/products/xlt/download.html
* Latest Firefox release: http://www.mozilla.org/en-US/firefox/new/

For automated WebDriver test execution with ANT:

* Latest [Google Chrome] (https://www.google.com/chrome/) release
* Latest ChromeDriver [binary] (http://code.google.com/p/chromedriver/downloads/list)
* Java [JDK] (http://www.oracle.com/technetwork/java/javase/downloads/index.html)
* [ANT] (http://ant.apache.org/bindownload.cgi)
or
* [Eclipse] (http://www.eclipse.org/downloads/)


~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
### 3. Getting Started

### 3.1. XLT Script Developer

* You'll find the scripdeveloper.xpi file in the XLT folder (tools)
* Install it as Firefox add-on (Firefox Add On Manager > Install add-on from file)
* Open the Script Developer and import the 'xcmailr_automation' folder as Test Project (folder button > Import...)
* Click on the play button to start a test run

Things you should consider, before you start the test execution:

* Settings: The tests were created and work with the default settings
* Modules: Use them, if you want to customize a test or create a new one
* Parameters: Many modules provide module parameters, which can be easily customized
* Language: The tests were designed for the English version of XCMailr
* Aol Mail: 
 * Aol mail accounts are used for sending and receiving forwarded emails and for all tests which require a registered XCMailr account
 * Feel free to use (but please do not abuse) the existing Aol accounts
  * See Global Test Data for credentials
 * You can also register your own aol.com, aol.de or aim.com email addresses
  * Set the 'Basic Version' of the webmail layout as 'Accessible Version' and disable the 'Spam Filter'
	
* Adapt the 'Global Test Data' (XLT Script Developer logo > Manage Global Test Data):
 * XCMailrURL: The URL of the XCMailr instance (English language parameter included!)
 * AolMailURL: The Sign In page of the English speaking Aol webmailer
 * AdminEmailAddress/AdminXCMailrPassword: XCMailr credentials of an arbitrary account with admin permission on XCMailr site, !!!choose an own before you start!!!
 * AolSenderAddress/AolSenderPassword: Aol webmail account to send emails to a temporary email address
 * AolReceiverAddress/AolReceiverPassword: Aol webmail account to receive forwarded emails
 * AolReceiverXCMailrPassword: Password of the receiving Aol mail account on the XCMailr site
 * AolTestAddress/AolTestPassword: Aol webmail account for XCMailr website tests, which require a registered user
 * AlreadyRegisteredAddress: Arbitrary email address which is registered at the XCMailr site (used for validation tests)

 
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
### 3.2 WebDriver Tests with ANT

The test suite also gained the ability to be executed from Eclipse or ANT directly by utilizing WebDrivers. 
Currently ChromeDriver and FirefoxDriver work fine. InternetExplorerDriver can be used, but is currently unstable.

The following steps explain quickly, how you can run the tests via ANT and use this either for build machines or a more automate local execution on your machine. 
This should work on any OS. Please make sure that you adjust the path names according to your OS style. 
The following examples are for OS X and Linux.

* Adjust the path to XLT in the `<testsuite>/build.properties` file: `xlt.home.dir = /home/user/xlt`
* Check `<testsuite>/config/project.properties`, pick the driver you want, adjust the other related properties:
 * Pick the driver to use: `webdriver = chrome`
 * Adjust the location of the Chrome binary: `webdriver.chrome.binary.location = /home/location/chromedriver`
 * Set the screen size if needed: `webdriver.screensize.[width|height] = 1200` 
 * You can also set the screensize for the responsive tests
* Run `ant test.java` and enjoy the magic happening.

At the end of the test, a JUnit report will be compiled and can be found in `<testsuite>/results/`.

On Windows you have to set some environment variables after ANT was installed.
* Right-click on 'Computer' in start menu or explorer
* Click on 'Properties'
* Click 'Advanced System Settings'
* Click 'Environment variables'
* Following 'System variables' have to be created/adapted:
 * PATH must point to \ant\bin directory
 * ANT_HOME must point to \ant directory
 * ANT_OPTS with value -Xmx256M


~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
4. Additional Help

XLT Script Developer documentation and Quick Start Guide: http://www.xceptance.com/products/xlt/documentation.html 
A short visual Script Developer introduction: http://youtu.be/Ykx4DcKo-mc
WebDriver with XLT: http://blog.xceptance.com/2013/04/23/webdrivers-in-xlt-how-to-run-test-cases-in-multiple-browser/