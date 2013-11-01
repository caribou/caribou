# Configuring Caribou

## How Configuration Works in Caribou

Caribou avoids holding any global state and elects rather to store state
particular to the application in a configuration map that is owned by the user.
This has a number of advantages, the first being that no code is tied to a
particular configuration.  Configurations can be swapped in and out and Caribou
will pick up and run with that configuration as if it had been using it the
whole time.

That given, there are a fair number of options and state that Caribou keeps
track of and requires to run, so not just any map will work.  In the
`caribou.config` and `caribou.app.config` namespace there are a number of
functions which facilitate the construction, modification and reading of these
configuration maps.

Once you have a configuration map, you can call any Caribou methods inside of a
`caribou.core/with-caribou` block.

```clj
(let [config (pull-config-map-from-somewhere)]
  (caribou.core/with-caribou config
    (... ))  ;; block of code that assumes a caribou config
```

As we progress we will illuminate a number of Caribou calls that work in this
manner.

Also, in order to access a value that lives inside a Caribou configuration, use
`caribou.config/draw`:

```clj
(caribou.config/draw :models :model :id) ---> The id of the Model model.
```

## Initializing a Caribou Configuration

In general, we will refer to namespaces inside a Caribou project as
`{project}.foo`, since we don't know what you named your project.  So if you
named your project "taiga" and we are talking about the `{project}.core`
namespace, that means `taiga.core`.

Caribou configuration is done by passing in a configuration map to the
`caribou.core/init` call in the main `{project}.boot` namespace.  By convention,
this map is obtained as a result of calling the
`caribou.config/config-from-environment` method on a default configuration map
obtained from `caribou.app.config/default-config`.

```clj
(let [default (caribou.app.config/default-config)
      local (config/merge-config default local-config)
      config (caribou.config/config-from-environment local)]
  (caribou.core/init config))
```

`caribou.core/init` sets up all the state that Caribou needs to run and stores
it in the config object passed into it.  Once a config map has been through
`caribou.core/init` it is ready to be used for any Caribou related operation
that needs to be performed.

`caribou.config/config-from-environment` just reads the result of whatever file
in `resources/config/{environment}.clj` matches the current environment setting
and merges that map into the default map you provide.  By default the
environment is "development", but it can be set as a java option (which can be
done in a number of ways).  One of the easiest is to set it in your env like so:

```bash
% export _JAVA_OPTIONS=-Denvironment=production
```

This is a standard method for setting JVM options from the command line.  (For
other methods check the java documentation).

There is also a `local-config` map that is defined in `boot.clj`.  It contains
the defaults for every config option that is possible in Caribou, and will be
applied to all environments before the config for that environment.  This
provides a way to set up sane defaults but still be able to modulate the
settings on an environment by environment basis.

Even though this is the default method for Caribou configuration, you can
configure Caribou in any way that gets a configuration map with the right
options into `caribou.core/init` in `{project}.boot`.  Your `{project}.core`
will call `{project}.boot/boot` to obtain this map when setting up the initial
handler.

## Configuration Options

Caribou is highly configurable in a number of ways.  Caribou configuration is
meant to work out of the box, while still allowing for any changes that might be
desired along the way.

### Default Configuration

There are a variety of options for configuring a Caribou site.  Most of these
you will not need immediately, but they are documented here for when they do
become necessary.

Here is a map of all default configuration options:

```clj
{:app {:use-database        true
       :public-dir "public"
       :default-locale "global"
       :localize-routes ""}
 :actions (atom {})
 :assets {:dir "app/"
          :prefix nil
          :root ""}
 :aws {:bucket nil
       :credentials nil}
 :cljs {:root "resources/clj"
        :reload true
        :options {:output-dir "resources/public/js/out"
                  :pretty-print true}}
 :controller {:namespace "{project}.controllers"
              :reload true
              :session-defaults (atom {})}
 :database {:classname    "org.h2.Driver"
            :subprotocol  "h2"
            :host         "localhost"
            :protocol     "file"
            :path         "/tmp/"
            :database     "taiga_development"
            :user         "h2"
            :password     ""}
 :error {:handlers (atom {})
         :templates (atom {})
         :show-stacktrace false}
 :field {:constructors (atom {})
         :namespace "{project}.fields"
         :slug-transform [[#"['\"]+" ""]
                          [#"[_ \\/?%:#^\[\]<>@!|$&*+;,.()]+" "-"]
                          [#"^-+|-+$" ""]]}
 :handler (atom nil)
 :hooks {:namespace "{project}.hooks"
         :lifecycle (atom {})}
 :index {:path "caribou-index"
         :default-limit 1000
         :store (atom nil)}
 :logging {:loggers [{:type :stdout :level :debug}]}
 :models (atom {})
 :nrepl {:port nil
         :server (atom nil)}
 :pages (atom ())
 :pre-actions (atom {})
 :query {:queries (atom {})
         :enable-query-cache  false
         :query-defaults {}
         :reverse-cache (atom {})}
 :reset (atom nil)
 :routes (atom (flatland/ordered-map))
 :template {:helpers (atom {})
            :cache-strategy :never}
}
```

As you can see, there is a whole rainbow of options to choose from.  Let's take
them one by one.

### app

Here is where we hold the most general configuration level options.

* **use-database**

Determines whether or not a database is in use.  Usually left at `true`.

* **public-dir**

The directory that holds all of the static resources a site contains.  Anything
placed in the public directory is available at the url representing its file
path without having to go through the router.

* **default-locale**

The name given to the default locale.  If you are not using localization you can
safely ignore this option.  If you are using localization, this is the locale
that is given to request maps if no other locale is specified.

### actions

This is an atom with a map containing all controller actions in the site.  You
probably won't have to interact with this one directly, unless you have custom
actions that are not defined in controller files.

### assets

Anything having to do with uploaded files is configured in this map.  The
available keys in the assets map are:

* **dir**

This specifies where local files on disk will be stored after upload.  "app/" by
default, could be anywhere on the filesystem.

* **prefix**

When using s3 for storing assets, this defines the prefix inside the bucket that
will be appended to the beginning of any asset path.  This provides a means to
have assets from many sites stored in a single bucket (if desired).

* **root**

The asset root can be used in templates to prefix a given asset with a different
host.  This way different environments can have assets that originate from
different hosts, like one set of assets for staging and one set for production
for example.

### aws

Information about how to connect to amazon is stored here.  Because the
configuration can be different for different environments, you could have one
amazon bucket or account for one environment, and a different account or bucket
for another environment.

* **bucket**

The name of the s3 bucket that assets will be stored in.

* **credentials**

A map containing your AWS credentials of the form `{:access-key
"YOUR-ACCESS-KEY" :secret-key "YOUR-SECRET-KEY"}`

### controller

The various options for configuring controllers.

* **namespace**

The namespace prefix where all of the controllers in your site live.  Defaults
to `{project}.controllers`, which means that any controller namespace you want
to reference must start with `{project}.controllers.{controller}`.  Actions are
functions inside your controller namespace, so the `index` action inside your
`home` controller in the `taiga` project would be found at
`taiga.controllers.home/index`.

* **reload**

Defaults to true.  This reloads every action on every request, which is helpful
in development when you are modifying them all the time, but you probably want
to turn it off in production unless you are modifying your controllers at
runtime (which is not suggested for production!)

* **session-defaults**

Anything placed into the session-defaults atom will be available in a fresh
session created when a user first visits your site.

### database

Any and all information for connecting to a database go in this map.  Usually
the main feature of each environment's config file, it holds a variety of
options, some of which are relevant only to certain databases:

* **classname** -- *required*

The Java class representing the driver for the database.  You can't really
connect to the db unless there is a class that handles the connection, which
there is for every database we have encountered.

* **subprotocol** -- *required*

This string represents the subprotocol that is used to connect to the database
through the driver.  Every driver has some specific options (usually only one).

Current possible values: postgresql, mysql, h2

* **host** -- *required*

What host does your database live on?  For local database development this will
most likely be `localhost`, but in many situations this is a remote server.

* **database** -- *required*

The actual name of your database.

* **user** -- *required*

The user that is being used to access the database.

* **password** -- *required*

The password that belongs to the given user.

* **protocol**

This is a string representing the mode the database is connected to with, if
applicable.  For instance, H2 can use file access, tcp access or a variety of
others.  Ignore if this does not apply.

* **path**

For accessing file based databases, this represents the location of your
database on disk.  Again, only necessary for file based databases.

#### Some example database configurations

Here are a couple of examples of database configurations to get you started:

* Postgresql

```clj
{:database
  {:classname "org.postgresql.Driver"
   :subprotocol "postgresql"
   :host "127.0.0.1"
   :database "caribou_test"
   :user "caribou"
   :password "TUNDRA"}}
```

* Mysql

```clj
{:database
  {:classname "com.mysql.jdbc.Driver"
   :subprotocol "mysql"
   :host "localhost"
   :database "caribou_test"
   :user "caribou"
   :password "TUNDRA"}}
```

* H2

H2 requires a couple more fields to identify that you are using a file based
database and to specify the path.  (notice `:protocol` and `:path` are both
present, but not `:host`)

```clj
{:database
  {:classname "org.h2.Driver"
   :subprotocol "h2"
   :protocol "file"
   :path "./"
   :database "caribou_development"
   :user "h2"
   :password ""}}
```

### error

When errors occur, these options governs how they are handled.

* **handlers**

This map holds custom error handlers for specific error codes.  So if you wanted
to do some custom action when a 404 is hit for instance, you could associate a
:404 key into this map with the value of a function to be run whenever a 404
occurs.  If no handler exists for that error, the default error handler is run.

* **templates**

A map holding templates that will be rendered in the case of various error
codes.  So a template that lives under the :404 key will be rendered whenever a
404 error occurs.

* **show-stacktrace**

Set this option to true if you want the stacktrace of any exception to appear in
the browser.  Not desirable for production when it is better practice to render
a custom 500 page, but in development this can be handy (especially if you
conjure a lot of stacktraces!)  Otherwise, the stacktrace is rendered out to the
logs and a 500 template is rendered in the browser.  Defaults to false.

### field

* **constructors**

A map that contains all the Field constructors.  Since Field is a protocol, to
create one requires calling a constructor.  This is a map of Field type names to
functions which construct a Field of that type.  Handled automatically by
Caribou, you probably don't need to mess with this, but it is here if you need
it.

* **namespace**

A namespace to hold any custom user-defined Field types.  Any records you define
that implement the Field protocol that live in this namespace will be added as
types that can be created like any other built in Field type.

* **slug-transform**

Whenever a piece of content of a Model with a Field of type "slug" is saved, the
value for that Field is generated from a linked text Field according to the
transformation encoded in this configuration property.  By default this
transformation removes quotes and turns special characters and spaces into a
dash (-).  Want underscores instead?  Override this config option.

### hooks

Hooks are run at specific point during every piece of content's lifecycle.  The
various hook points are:

* **During create these hooks are called in order:**

```clj
:before-save
:before-create
:after-create
:after-save
```

* **During an update, these hooks are called in order:**

```clj
:before-save
:before-update
:after-update
:after-save
```

* **When a piece of content is destroyed, these hooks are run:**

```clj
:before-destroy
:after-destroy
```

* **namespace**

The namespace where the various hooks into the Model lifecycle go.  Every hook
namespace has a name of the form {hooks-namespace}.{model-name}, and hooks are
added in a function called {hooks-namespace}.{model-name}/add-hooks.

* **lifecycle**

The actual hooks that get run.  Rather than modifying this directly, just call
`caribou.hooks/add-hooks` from a file named after that model in your hooks
namespace.

### index

The index options control how content is indexed in the built in Lucene search
engine.  This is used in the Admin but you can also use it in your own site.
http://lucene.apache.org/

Caribou uses the clucy library to abstract over the raw Java Lucene interface:
https://github.com/weavejester/clucy

* **path**

The directory that will hold the index files.  Defaults to "caribou-index".

* **default-limit**

The maximum number of documents a search will return.  Defaults to 1000.

* **store**

An atom of the actual clucy index object, if you need to perform any custom
operations on it.

### logging

Logging contains a list of logger specifications under the :loggers key.  These
specifications are a map containing two keys: `:type` and `:level`.  `:type`
indicates what endpoint the logger will output to (the default logger writes to
:stdout), and `:level` indicates what level of Caribou events to pay attention
to.

The different types currently supported are `:stdout`, `:file` and `:remote`.
`:stdout` simply outputs to stdout, and is the default logger type.  If `:file`
is chosen, you must also add a `:file` key to the map pointing at the file to
log to.  If the logger type is `:remote` then you must also include a `:host`
key which indicates what remote host to log to.  In the case of a remote host,
it uses UDP to send packets to the host, so the host must be running syslog and
must be configured to allow access from the server sending the packets.

The levels in order from most critical to least critical are:

```clj
:emergency 0
:alert 1
:critical 2
:error 3
:warning 4
:warn 4
:notice 5
:informational 6
:info 6
:debug 7
:trace 7
```

If you set a logger to watch at `:warn` level for instance, it will ignore any
event below `:warn`, but output all messages from `:warn` level up to
`:emergency`.  `:emergency` level events are always output.

### models

This is a map that contains all Models in the system.  During a call to
`caribou.core/init` the Models are loaded from memory and added to this map
under a key containing the slug of the Model.  If you want to define Models that
are not represented in the Model table, you can add more keys to this map
(though this is probably unnecessary).

There is a whole section on [Creating Models](models.md) later on.

### nrepl

Nrepl provides a repl running inside the Caribou process that can be connected
to from the command line or from inside an editor with
[nrepl](https://github.com/clojure/tools.nrepl) support.  This is a great way to
interact with a running Caribou process and inspect or alter state using a given
configuration.

If a `:port` is provided, then an nrepl server will be booted at that port when
Caribou is initialized.  In that case, a reference to the running server will be
stored in the atom under `:server`.  If no `:port` option is present, nrepl will not
be booted.

### pages

This provides a reference to the page tree for this Caribou instance.  Most
likely this will be populated during the definition of the handler in your
`{project}.core` namespace.  `{project}.core/reload-pages` is a function that
adds whatever routes you have to your site, which gets passed into the
invocation of the root handler, `caribou.app.handler/handler`, so that it can
reload the pages whenever necessary.  This is all covered in the section on
[Defining Routes and Pages](routes.md).

### pre-actions

This configuration option holds the current map of existing pre-actions for
different pages.  Keyed by the slug of a page, pre-actions will be run before a
given action is evaluated.  This could be used for things like authorization or
processing of request parameters.  See the section on
[Defining Pre-Actions](controllers.md) in the documentation for controllers for
more details.

### query

The `query` option is the domain of the query cache.  Turned off by default, the
query cache will cache the results of every query map that
`caribou.model/gather` sees.  There are a variety of entries in this map that
play different roles in the inner workings of the query cache.

* **enable-query-cache**

To turn on the query-cache, simply set this option to `true` in your config.
Not necessary for development, but a good thing to do in production if you know
that your content is not necessarily changing often.  Even if it does change,
the cache will be invalidated on any update to that model, so your site will
remain current.

* **queries**

An atom containing the map of queries to results.  Populated automatically by
the query cache (but fun to inspect, if you are into that kind of thing).

* **reverse-cache**

Tracks the models that are hit by each query.  Invalidates those caches in the
case of an update.

* **query-defaults**

This map will be added automatically to any query issues through a
`caribou.model/gather`.  Want to restrict your content to only those "enabled"?
This is the place to do it.

### reset

This is a reference to a user-defined function passed into the initial creation
of the frontend Caribou handler in your `{project}.core/init` function.  It
should do any kind of necessary initialization work that your site requires
(like loading pages or defining routes, for instance).  It is added
automatically in a newly generated Caribou site.

### routes

This is an ordered map of your routes.  The routes map url patterns to the
actions that are triggered by them.  One by one each pattern is tested against
an incoming url until it is matched or a 404 is issued.  Once a route is matched
the corresponding action is called with the request map as a parameter.  See
more at [Defining Routes and Pages](routes.md).

### template

The various options pertaining to the built-in template rendering live here.

* **cache-strategy**

This option governs the caching strategy used by the template engine.  The
possible values are currently `:never` or `:always`.

* **helpers**

This is a map containing the default helpers that will be available during the
rendering of every template.  To find out all about helpers check out the
section on [Template Helpers](templates.md).

