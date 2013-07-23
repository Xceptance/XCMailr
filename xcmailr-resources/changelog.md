# Changelog
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