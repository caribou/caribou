# Miscellaneous Topics

## Connecting to a Caribou Repl

Caribou provides an embedded nrepl server that lives inside the context of the
currently running configuration.  This means you won't have to call every
`caribou.model/gather` or `caribou.model/create` inside a
`caribou.core/with-caribou` call.

For a new Caribou project the repl is enabled by default.  It lives under the 
configuration option 

```clj
{:nrepl {:port 44444}}
```

If you want to disable this simply remove this entry and the nrepl server will
disappear.  To use it, specify a port (44444 by default) and connect using your
favorite nrepl client!

## Search Indexing



## Query Cache

There is an optional query cache available in Caribou that you can use to avoid
executing the same query over and over again if the content fetched by that
query hasn't changed.  The model system keeps track of each model that was
involved in a given query and it knows whenever content for a given model
changes, so it can automatically expire the cache for any queries tied to a
model involved in a recent update.  This way, all the results can be returned in
constant time without hitting the database for any content that hasn't changed,
which in an environment with more reads than writes is a huge win.

To enable the query cache, simply set the `:enable-query-cache` key to true in
your config:

```clj
{:query {:enable-query-cache true}}
```

Caribou will handle the rest.  

