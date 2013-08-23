# Getting Started

## System Requirements

To run Caribou you will need: 

* A [JVM](http://www.java.com/en/download/help/index_installing.xml) (6 or higher)
* [Leiningen](http://leiningen.org/) (at least 2.0)

If you can run `lein help`, you are ready to go!

## Installing Caribou

Everything you need to run Caribou is just a `lein` plugin.  It will fetch and
install all the necessary dependencies for you.

* Create a file called `~/.lein/profiles.clj` with the following contents:

```clj 
{:user 
  {:plugins [[lein-ring "0.8.6"] 
             [antler/lein-caribou "2.4.2"]]}}
```

* (note these versions may have increased.  Check [Clojars](http://clojars.org) for latest
  version information)

That's it!  You are now ready to create a site.

## Creating a New Site

To create a new Caribou project, type this at the command line:

``` 
% lein caribou create taiga 
```

This will create a new directory structure under the name `taiga` and prime a
new H2 database for use with Caribou.

If you don't want to use H2 you can configure Caribou to use other database
backends.

## Running the Site

To run the site as it exists, simply:

```
% cd taiga
% lein ring server
```

A new window will appear in your browser under [http://localhost:33333](http://localhost:33333).  

Congratulations!  You are now running Caribou.

