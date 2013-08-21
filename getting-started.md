# Getting Started

## Installing Caribou

Caribou depends on the java runtime, so the zeroth step would be to
[install a JVM](http://www.java.com/en/download/help/index_installing.xml) if
you don't have one already.

Next, [install Leiningen](http://leiningen.org/) (which provides the `lein`
command) if it is not already installed.

Once you have the `lein` command, create a profile that includes `lein-caribou`
(the Leiningen Caribou plugin).

* Create a file called `~/.lein/profiles.clj` with the following contents:

```clj 
{:user 
  {:plugins [[lein-ring "0.8.6"] 
             [lein-caribou "2.3.0"]]}}
```

* (note these versions may have increased.  Check http://clojars.org for latest
  version information)

* Run `lein help` to test out your setup.  If you see some helpful output you
  are ready to go!

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

A new window will appear in your browser under http://localhost:33333 .  

Congratulations!  You are now running Caribou.

