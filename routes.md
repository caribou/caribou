# Defining Routes and Pages

When a user visits a URL in your site, the Caribou Router is what matches that
URL and sends a request to a particular controller you have defined in your
`src/{project}/controllers` directory.  These controllers are just Clojure
namespaces which contain a collection of functions (which we call "actions"),
each of which can conjure a response based on a given request.

In order to perform this magic, you have to specify which URLs map to which
controller actions, and what parts of that URL are parsed and provided to the
action in the form of parameters.  This happens through Routes.

Routes define a routing hierarchy which is based on URL paths.  Every route
defines a path (which is a string to match), a key which uniquely identifies
that route, and a set of child routes, each of which inherits the first part of
its path from its parent.  This tree will then be used by the router to route
requests based on their URL to the controller actions given by that route's key.

The simplest route would be one that matches the empty path, "/", and maps to a
home page.  This is given below:

```clj
["/" :home {:GET {:controller 'home :action 'home :template "home.html"}} []]
```

The path is "/", the key is `:home`, it matches the HTTP `GET` method and
responds by passing the request to the `home` action in the `home` controller,
and it has no child routes (the empty vector at the end is optional if the route
has no children).  Needless to say, routes for a site can become much more
elaborate than this, but they are all represented in this same format.

The above is a single route, but in practice routes come as a collection.  So an
example of the simplest routing a site could have would be something like the
following:

```clj
(def routes
  [["/" :home {:GET {:controller 'home :action 'home :template "home.html"}}]])
```  

In this case, a route will be triggered by any request with the uri "/", and the
next map discerns which methods this page will match.  So in the case of a `GET`
request, the corresponding controller that will be activated is the `home`
controller, which is located in `src/{project}/controllers/home.clj` in the
`{project}.controllers.home` namespace, and the action that will be called will
be a function by the name of `index` defined in that namespace.

As you can imagine, there could be several routes living in parallel:

```clj
(def routes
  [["/" :home {:GET {:controller 'home :action 'index :template}}]
   ["/place" :general-place {:GET {:controller 'home :action 'general :template "general.html"}}]
   ["/place/:name" :specific-place {:GET {:controller 'home :action 'specific :template "specific.html"}}]])
```  

There can be multiple methods if desired:

```clj
(def routes
  [["/" :home {:GET    {:controller 'home  :action 'index  :template "home.html"}
               :POST   {:controller 'home  :action 'login  :template "login.html"}
               :PUT    {:controller 'home  :action 'update :template "acknowledge.html"}
               :DELETE {:controller 'hades :action 'perish :template "writhing.html"}}]])
```

Once we have a set of routes that refer to some controller methods, we bind them
to the actual methods in a subsequent step.  `caribou.app.pages/bind-actions`
takes a set of routes and a namespace and swaps out references to functions with
the actual functions:

```clj
(caribou.app.pages/bind-actions routes namespace)
```

This can later be given to the initialization of the Caribou handler that will
be running your site.  It will define the routing structure and URL matching
that will be followed by the running app.

## Routes are Matched based on Paths

The routes you define govern the way URLs coming from requests will be matched.
So given a set of routes, you can tell how an incoming URL will be handled.
Take the following case:

```clj
(def routes
  [["/"               :home      ...]
   ["/place"          :place     ...]
   ["/somewhere-else" :somewhere ...]])
```  

(In this and most subsequent routing examples the details of the method map and
controller information will be replaced by `...` for clarity).

Here there are three separate routes.  Any incoming request will match one of
these routes, or trigger a 404.  Caribou routes match given a trailing slash or
not, so:

```bash
http://localhost:33333                   --->  :home
http://localhost:33333/                  --->  :home
http://localhost:33333/place             --->  :place
http://localhost:33333/place/            --->  :place
http://localhost:33333/somewhere-else    --->  :somewhere
http://localhost:33333/somewhere-else/   --->  :somewhere
http://localhost:33333/off-the-map       --->  404!
```

## Route Elements can be Variable

This is all well and good, but what if you want to pull up a model by id?  Do
you need a route for every id that could be called?

This is where variable slugs come into play.  You can specify a placeholder path
element with a `:`, and when the router matches it it will parse the path and
pass the value in as a named parameter.

Here is an example:

```clj
(def routes
  [["/"            :home           ...]
   ["/place"       :general-place  ...]
   ["/place/:name" :specific-place ...]])
```

In this case, the router will match any URL of the form "/place/*" and assign
whatever the * is to a parameter called `:name`.  So:

```bash
http://localhost:33333                   --->  :home
http://localhost:33333/place             --->  :general-place
http://localhost:33333/place/hello       --->  :specific-place  {:name "hello"}
http://localhost:33333/place/earth       --->  :specific-place  {:name "earth"}
```

Once the request reaches your controller, you can access the value of `:name` in
the request map:

```clj
;; request to http://localhost:33333/place/earth

(defn place
  [request]
  (println (-> request :params :name)))
  
---> "earth"
```

One word of caution: a variable slug can shadow a specific slug, so the ordering
of your routes matters:

```clj
(def routes
  [["/place/:where"  :variable-place ...]    ;; <--- absorbs all requests
   ["/place/here"    :right-here     ...]])  ;; <--- never called!
```

This is easily resolved by swapping the order:

```clj
(def routes
  [["/place/here"    :right-here     ...]    ;; <--- now this works
   ["/place/:where"  :variable-place ...]])  ;; <--- called only if the previous route fails to match
```

## Routes can be Nested, Paths are Inherited

A useful feature for organizing routes is to decompose them into a hierarchy.
Routes inherit their path from the routes above them in the hierarchy, which
means subtrees can be moved around and put into new places in the hierarchy
while preserving the routing structure of that subtree.  Every subroute just
needs to know its own path and what routes it has as children, and the full path
is implied by its position in the tree.

Here is an example:

```clj
(def routes
  [["/"                    :home                 ...
    [["presentations"      :presentations        ...
      [[":presentation"    :presentation-detail  ...
        [["info"           :presentation-info    ...]
         ["author/:author" :presentation-author  ...]
         ["slides"         :slides               ...
          [[":slide"       :slide-detail         ...]]]]]]]
     ["categories"         :categories           ...
      [[":category"        :category-detail      ...]]]]]])
```

This generates a moderately comprehensive routing structure for a
presentation-based application.  Here is a representative sample of routes that
will be matched by this routing tree:

```bash
.../                                      --->  :home
.../presentations                         --->  :presentations
.../presentations/caribou                 --->  :presentation-detail {:presentation "caribou"}
.../presentations/caribou/info            --->  :presentation-info   {:presentation "caribou"}
.../presentations/caribou/author/tundra   --->  :presentation-author {:presentation "caribou" :author "tundra"}
.../presentations/caribou/slides          --->  :slides              {:presentation "caribou"}
.../presentations/caribou/slides/welcome  --->  :slide-detail        {:presentation "caribou" :slide "welcome"}
.../presentations/caribou/slides/routing  --->  :slide-detail        {:presentation "caribou" :slide "routing"}
.../categories                            --->  :categories
.../categories/programming                --->  :category-detail     {:category "programming"}
```

Paths are generated for each route based on the sequence of paths starting at
the root of the tree leading to that route.  This makes it easy to define
sub-parts of your application's routing structure as individual trees and then
compose them however you want.  The following is equivalent to the above routing
tree:

```clj
(def slide-routing
  ["slides"    :slides       ...
   [[":slide"  :slide-detail ...]]])

(def presentation-routing
  ["presentations"       :presentations        ...
   [[":presentation"     :presentation-detail  ...
     [["info"            :presentation-info    ...]
      ["author/:author"  :presentation-author  ...]
      slide-routing]]]])

(def category-routing
  ["categories"   :categories      ...
   [[":category"  :category-detail ...]]])

(def all-routes
  [["/"  :home  ...
    [presentation-routing
     category-routing]]])
```

This kind of separation of concerns allows for clean decomposition of different
aspects of the routing structure, and also enables the addition of libraries
which define their own routes to be inserted at arbitrary points in your own
routing tree.  Not all routes need to be defined up front, and not all defined
routes need to know where they are ultimately going to live.  Think of it as
functional decomposition of the routing structure of your application.

## Methods Map Routes to Controllers and Templates

A page is indexed by its HTTP method, so that the same route can map to
different controller actions based on whether it is a GET or a POST or whatever
else.

The method map itself contains two keys at minimum: `:controller` and `:action`.
It can contain any keys you wish and those keys will be available at render time
in the request map under `:page`, but at least it must guide the system on which
controller and action to pass any matched request at run time.  In addition, if
your action is going to make use of the built in rendering then it must also
contain a `:template` key that specifies which template to render.

Putting this all together, the simplest method map looks like this:

```clj
{:GET {:controller 'home :action 'index :template "index.html"}}
```

There is one page, `:home`, that responds to one method, `:GET`, and routes the
request received to the "index" action inside the "home" controller.  Once there, if 
`caribou.app.controller/render` is called in that controller, the template
"index.html" living inside your "resources/templates" directory will be rendered
with whatever map is passed into the `render` call.  This is the full round-trip
story of Caribou routing, from request to route matching to controller action to
template rendering and back as a response.  This is the pattern of the Internet.  

## Providing your Pages to the Caribou Handler

Once you have acquired a set of routes, you can add it into a list with a bunch
of other routes.  This is already happening inside your `{project}.core`
namespace in the `{project}.core/reload-pages` function (this is where the Admin
and the API are added into your site).  This function is eventually handed to
the core Caribou handler that runs your site so that all routes can be reloaded
when necessary:

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

The first two calls to `pages/convert-pages-to-routes` add in the Admin and API
routes respectively.  The last one is currently adding in all the pages defined
in the database (usually created through the Admin) by calling
`pages/all-pages`, but you can give it any page tree you have created here.

Notice also that `pages/convert-pages-to-routes` has a number of additional
arguments that can be passed in.  The first argument is a page tree, and the
second is the controller namespace.  If you want to move where you store your
controllers you can change this in your config, or just hardcode something here
(like was done for the Admin and API, each of which have controller namespaces
that live inside those respective projects).  The third argument is a URL
prefix, which is how all the Admin and API routes end up living under "/\_admin"
and "/\_api".

An example `{project}.core/reload-pages` that does not include the Admin or API
but does use your custom routes using a custom controller namespace
and a different URL prefix would look something like this:

```clj
(def routes
  [["/" :home {:GET {:controller 'home :action 'index :template "home.html"}}]])

(defn reload-pages
  []
  (pages/bind-actions
   [["/some-kind-of-prefix" :prefix {} page-tree]]
   'some.other.controller.namespace))
```

Of course if you want to use the default controller namespace and have your
routes live at the root, it is as simple as:

```clj
(defn reload-pages
  []
  (pages/bind-actions
   routes
   (config/draw :controller :namespace)))
```

