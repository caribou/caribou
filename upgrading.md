# Upgrading Caribou from a Previous Version

Caribou is changing fast.  Every version adds things and takes things away
(hopefully in a way that makes it better!)  As time goes on things will stabilize,
but for now its alpha status means that breaking changes may occur at any time.

This document exists to facilitate people who have created a site using one
version who want to upgrade to a newer version.  Rather than let you flail in
the dark, we have attempted to make this transition smoother.  While we will
endeavor to make this document as complete as possible, let us know if you find
anything that is missing!

## Upgrading from 0.13.\* to 0.14.\*

### Versions

First, you have to make some updates to your `project.clj`.  We have removed the
Immutant dependency (since you can run it directly from information provided in
`project.clj`!) and added
[Schmetterling](https://github.com/prismofeverything/schmetterling), which is an
entirely optional debugger (so only add it if you want debugging support).

#### Old way:

```clj
:dependencies [...
               [org.immutant/immutant "1.0.2"]
               [caribou/caribou-admin "0.13.0"]
               [caribou/caribou-api "0.13.0"]
               ...]
:plugins [...
          [caribou/lein-caribou "2.13.0"]
          [lein-cljsbuild "0.3.3"]]
:immutant {:context-path "/"
           :init {{name}}.immutant/init}
```

#### New way:

```clj
:dependencies [...
               [caribou/caribou-admin "0.14.0"]
               [caribou/caribou-api "0.14.0"]
               [schmetterling "0.0.8"]
               ...]
:plugins [...
          [caribou/lein-caribou "2.14.0"]
          [lein-cljsbuild "1.0.2"]]
:immutant {:context-path "/"}
```

Also, there has been a lot of work to enable Heroku support.  Part of that is
providing an uberjar profile:

```clj
  :uberjar-name "{{project-name}}-standalone.jar"
  :profiles {:uberjar {:aot :all}}
```

### Routes

One of the biggest changes in the 0.14.\* version is that routing has been
drastically simplified with the inclusion of
[Polaris](https://github.com/caribou/polaris).  Before there was a concept of
distinct "routes" and "pages" which would later be merged and matched by their
keyword identifier.  This has been consolidated into a single "routes" vector,
which has been pretty unanimously appreciated by everyone. 

From `routes.clj`
#### Old way:

```clj
(def routes
  [["/"            :home           []]
   ["/place"       :general-place  []]
   ["/place/:name" :specific-place []]])

(def pages
  {:home           {:GET  {:controller 'home :action 'index :template "index.html"}}
   :general-place  {:GET  {:controller 'home :action 'general :template "general.html"}
                    :POST {:controller 'home :action 'general-post :template "general-response.html"}}
   :specific-place {:GET  {:controller 'home :action 'specific :template "specific.html"}}})

(def page-tree
  (caribou.app.pages/build-page-tree routes pages))
```

#### New way:

```clj
(def routes
  [["/" :home {:GET {:controller 'home :action 'index :template}}]
   ["/place" :general-place 
             {:GET (fn [request] 
                     (lets-respond-directly-to-this (do-stuff-to request)))
              :POST {:controller 'home :action 'general-post :template "general-response.html"}}]
   ["/place/:name" :specific-place
             {:GET  {:controller 'home :action 'specific :template "specific.html"}}]])
```

Things to note:

* No more matching keywords across two different structures!  Yay!

* No need to have the empty vector of child routes if there aren't any.

* You can now provide functions directly to the routes, rather than being forced 
  to reference functions from a controller namespace.

This change has consequences for the rest of your `routes.clj` and `core.clj` as well.

From `routes.clj`, the old way:

```clj
(defn page-tree
  []
  (pages/build-page-tree routes pages))

(defn gather-pages
  []
  (let [db-pages (try 
                   (pages/all-pages)
                   (catch Exception e nil))]
    (pages/merge-page-trees db-pages (page-tree))))
```

#### New way:

```clj
(defn build-routes
  [routes namespace]
  (pages/bind-actions routes namespace))

(defn gather-pages
  []
  (try 
    (pages/all-pages)
    (catch Exception e nil)))
```

Before, you would take the routes and pages and merge them into a page tree,
then get the pages from the database and merge the two trees for processing in
`core.clj`.  Now you simply bind the routes to their controller functions and
return the database pages separately.

This comes to fruition in `core.clj`, where all the routes are added to the handler.

#### Old way:

```clj
(defn reload-pages
  []
  (pages/add-page-routes
   admin-routes/admin-routes
   'caribou.admin.controllers
   "/_admin"
   admin-core/admin-wrapper)

  (pages/add-page-routes
   api-routes/api-routes
   'caribou.api.controllers
   "/_api"
   api-core/api-wrapper)

  (pages/add-page-routes
   (routes/gather-pages)
   (config/draw :controller :namespace)))
```

#### New way:

```clj
(defn reload-pages
  []
  (concat 
   (pages/convert-pages-to-routes
    admin-routes/admin-routes
    'caribou.admin.controllers
    "/_admin"
    admin-core/admin-wrapper)

   (pages/convert-pages-to-routes
    api-routes/api-routes
    'caribou.api.controllers
    "/_api"
    api-core/api-wrapper)

   (routes/build-routes
    routes/routes
    (config/draw :controller :namespace))

   (pages/convert-pages-to-routes
    (routes/gather-pages)
    (config/draw :controller :namespace))))
```

This may seem more verbose, but that is because the routes from `routes.clj` are
being added separately from any routes created through making Pages in the
Admin.  The new `pages/convert-pages-to-routes` reflects the fact that before,
routes were being converted to their database format in order to be added to the
handler, whereas now the reverse is happening and the database pages are being
converted into [Polaris](https://github.com/caribou/polaris) routes.  

### Helpers

Another difference in `core.clj` is that built-in template helpers are now
provided through a middleware, and you can provide your own helpers from a new
file `helpers.clj`.

#### Old way:

```clj
(:require [caribou.app.helpers :as helpers])

...

(defn provide-helpers
  [handler]
  (fn [request]
    (let [request (merge request helpers/helpers)]
      (handler request))))

...

(-> (handler/handler reload-pages)
    (provide-helpers))
```

#### New way:

```clj
(:require [caribou.app.helpers :as helpers]
          [{{project-name}}.helpers :as user-helpers])
          
...

(-> (handler/handler reload-pages)
    (helpers/wrap-helpers user-helpers/additional-helpers))
```

The new `helpers.clj` file looks like this:

```clj
(ns {{project-name}}.helpers)

(def additional-helpers
  {:hello 
   (fn [x] 
     (str "Hello " x "!"))})
```

You can add any helpers you need in your templates here.  

### Config

The last change is that configuration has been enhanced to accept a database
connection from an environment string if it is present.  This is another change
to support Heroku deployment, and makes the whole thing fairly seamless.  Even
with this change, you shouldn't see any difference if you are not deploying to
Heroku.

#### Old way:

```clj
(defn boot
  []
  (let [default (app-config/default-config)
        local (config/merge-config default local-config)
        config (config/config-from-environment local)]
    (caribou/init config)))
```

#### New way:

```clj
(defn boot
  []
  (-> (app-config/default-config)
      (config/merge-config local-config)
      (config/config-from-environment)
      (config/merge-db-connection {:connection "DATABASE_URL"})
      (config/process-config)
      (caribou/init)))
```

Cleaner with threading as well?  You decide.

### Feedback

There are many more features and improvements in the new version, but that
should be everything you need to do to upgrade your Caribou instance from
0\.13\.\* to 0\.14\.\*.  Please let us know if you run into any additional
issues!  And let us know anything else for that matter.

Thanks for using Caribou!
