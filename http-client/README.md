# Perma query - Java Client

This is a java app which uses Apache HttpClient to query one url specified by the env variable `URL` (by default it is 'http://httpbin.org/get'').
The client can be configured to use timeout (i.e. specify the keep alive, by using the env variable `WITH_TIMEOUT` and setting it to true)

