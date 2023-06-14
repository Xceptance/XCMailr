# Changelog
## Version 1.1.5
* switch to latest ninja-framework 3.3.3
* switch from bootstrap 2 to 3
* updated jquery to version 1.11.1
* updated ui-bootstrap to version 0.11.2
* added bootstrap3-typeahead, as the new bootstrap has removed its own typeahead-component and recommends using twitter-typeahead (which is not working well with bootstrap, due to css-problems) see: https://github.com/bassjobsen/Bootstrap-3-Typeahead
* minor fixes
* refactoring of all java classes

## Version 1.1.4
* new UI for the email-address handling using angularJS
* fixed a bug in the session handling (changes to the user-profile will be immediately visible)
* switch to ninja-framework 1.6.0
* two ways to handle emails (fix for #21: https://github.com/Xceptance/XCMailr/issues/21 )
 * first way: replace the original sender and put him into the header as new field "X-Forwarded-From", the new sender will be your temporary address
 * second way: create a new message wrapping the original email, containing the header as part of the new message (like the "forward message"-function in thunderbird)
* some minor improvements 

### Changes in application.conf 
* added the key "mail.msg.rewrite" which holds a boolean-value 
 * false: original sender will only be set as field "X-FORWARDED-FROM"
 * true: the original message will be wrapped in a new one 
 * if unset, the default value will be false

## Version 1.1.3
* New language handling in getLanguageList-method
* Some small adjustments in application messages
* Highlighting of current-menu-entries
* Performance improvements for the mailtransaction-log (faster deletion, logging via queue)
* Update to ninja-framework 1.5.1 
 * added use of their caching-implementation (internal EhCache for dev and test-modes, external MemCached for prod-mode)
 * switched to the use of their new NoHttpBody improvement for redirections
* Creation of a Changelog file

### Changes in application.conf
* Due to the changes in caching, the key "memcached.port" isn't used anymore, additionally the key "memcached.host" now needs additionally the port of the memcached server (e.g. the value is then: localhost:11211)
* additionally, you can set the values "ninja.cache.CacheMemcachedImpl" or "ninja.cache.CacheEhCacheImpl" for the key "cache.implementation" to set either an external Memcached-Server or an included EhCache-implementation
 * for productive mode (normally used if you start the application with the run.sh-script), you should always use memcached!