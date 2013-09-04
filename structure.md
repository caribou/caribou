# Default Directory Structure

The Caribou directory structure is designed to be simple and flexible.  
Running `tree` in the root illuminates the structure:

    ├── app
    │   └── assets
    ├── project.clj
    ├── resources
    │   ├── cljs
    │   │   └── taiga.cljs
    │   ├── config
    │   │   ├── development.clj
    │   │   ├── production.clj
    │   │   ├── staging.clj
    │   │   └── test.clj
    │   ├── public
    │   │   ├── css
    │   │   │   ├── fonts
    │   │   │   │   ├── caribou.eot
    │   │   │   │   ├── caribou.svg
    │   │   │   │   ├── caribou.ttf
    │   │   │   │   └── caribou.woff
    │   │   │   └── taiga.css
    │   │   ├── favicon.ico
    │   │   └── js
    │   │       └── taiga.js
    │   └── templates
    │       ├── errors
    │       │   ├── 404.html
    │       │   └── 500.html
    │       └── home.html
    ├── src
    │   └── taiga
    │       ├── boot.clj
    │       ├── controllers
    │       │   └── home.clj
    │       ├── core.clj
    │       ├── hooks
    │       │   └── model.clj
    │       ├── immutant.clj
    │       ├── migrations
    │       │   ├── admin.clj
    │       │   ├── default.clj
    │       │   └── order.clj
    │       └── routes.clj
    ├── taiga_development.h2.db
    ├── taiga_development.trace.db

There are some main features to take note of for now.

### project.clj

First is the `project.clj`, which configures `lein` and holds information about
dependencies and plugins.  You will be editing this when you want to add a new
Clojure library to your project, for instance.  Also, this is where you define
various options about how the site runs, including the port, the handler and an
init function that is run on boot.  Full details can be found in the
configuration section on project.clj.

### resources

The `resources` directory has four branches: `cljs`, `config`, `public`, and
`templates`.

* **cljs**

This directory holds any clojurescript files for your Caribou project.  By
default these files will be compiled to javascript every time they are changed. 

* **config**

`config` holds all the configuration files for the various environments that
your Caribou app will eventually run in.  The name of each environment maps to a
configuration file with the same name and suffixed by `.clj`.  So in the
"development" environment Caribou will use the `development.clj` config file.
For now the app defaults to `development`, but there are things you will want to
shut down for production that are helpful in development, like automatic code
reloading.  For this Caribou provides a `production.clj` with its own set of
configuration options.

* **public**

Anything in `public` will be accessible as a static resource under the URL that
maps to this directory structure.  If all you have is static content, just throw
a bunch of files in here where you want them to be accessed and you are good to
go!  We have put some helpful files in here to get you started, (css and js) but
nothing is set in stone.  Have at!

* **templates**

Here is where all of the dynamic templates go.  In Caribou, you can create
content that can then be accessed from templates.  Caribou uses a template
engine called [Antlers](https://github.com/caribou/antlers) by default.  

### src

`src` holds all of the Clojure files that run your Caribou site.  Inside is a
directory named after your project (here that is "taiga").  All of your site
specific code will go in here.

There are some notable entries in your project source folder:

* **core.clj**

This is the entry point into your Caribou project, and ultimately what gets
executed on boot.  You can change everything about how Caribou runs from inside
this file, from replacing pages and models to defining configuration to
executing handlers for every request.  For now, the structure is set up to run
Caribou how it was designed to be run, but never forget that you have ultimate
control of this.

* **boot.clj**

This file governs which configuration file gets loaded.  You can also change
configuration options inside this file that apply to all running environments,
if you wish.

* **immutant.clj**

There is an `immutant.clj` for configuring [Immutant](http://immutant.org/)
(which is an optional app container).  If you are not using Immutant you don't
need to worry about this one.

* **migrations**

This directory contains data migrations that specify how your data evolves over
time.  You can add your own migrations in addition to the migrations necessary
to run your site for the first time.  Any migration files added here must be
included in `order.clj`.  This is necessary so that the migration system knows
what order to run the migrations in.  The database keeps track of which
migrations have been run, so no migration is ever run twice on one database.

* **hooks**

Hooks are defined per model.  There are a variety of points in the content
lifecycle where custom code can be run, if desired.  This is covered in the
configuration section on hooks.

* **controllers**

Controllers are called when an http request is matched by a page that references
that controller.  This is how routes are linked to actual Clojure functions that
eventually render a template or a return a valid response of some kind.

### The default H2 database

This will be named after your project with the suffix "_development.h2.db".  By
default Caribou uses H2 because it is an all java database which requires no
native dependencies.  You will probably want to swap this out with your own
database backend, but Caribou will work fine if all you ever want to use is H2.

