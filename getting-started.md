# Getting Started

## System Requirements

To run Caribou you will need:

* A [JVM](http://www.java.com/en/download/help/index_installing.xml) (6 or higher)
* [Leiningen](http://leiningen.org/) (at least 2.0)

If you can run `lein help`, you are ready to go!
*Note*: You do *not* need to install Clojure!

## Installing Caribou

If you have Leiningen installed, there is nothing more to install!

## Creating a New Site

To create a new Caribou project, type this at the command line:

```bash
    % lein new caribou taiga
```

This will create a new Caribou directory structure under the name `taiga`.  Site
created!

## Bootstrapping a Database

To bootstrap a fresh database for Caribou to use, simply:

```bash
    % cd taiga
    % lein caribou migrate resources/config/development.clj
```

Bootstrapped!

You can run Caribou without a database if you just want the routing, controllers
and template rendering, but you need a database to use the Admin or API and much
of the other functionality.

By default Caribou uses [H2](http://www.h2database.com/html/main.html) (an all
Java database engine) so that it does not depend on anything besides the JVM.
If you don't want to use H2 you can configure Caribou to use other database
backends.  See the section on [database configuration](configuring.md) for more
on how to do this.

## Running the Site

To run the site as it exists, simply:

```bash
    % lein ring server
```

A new window will appear in your browser under [http://localhost:33333](http://localhost:33333).

Congratulations!  You are now running Caribou.

