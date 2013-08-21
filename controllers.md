# Writing Controllers

The whole point of the Caribou router is to funnel requests to the appropriate
controller action with the right parameters.  Once a request comes through, an
action is simply a function that is called with the request map as an argument,
and evaluates to a valid response map.  In between of course, all kinds of magic
can happen.

## Controllers are Namespaces which contain Actions

To define a controller namespace, add a new file to your
`src/{project}/controllers` directory with the name of the new controller.  So
for this example it would be `src/taiga/controllers/example_controller.clj` with
the following contents:

```clj
(ns taiga.controllers.example-controller
  (:require [caribou.model :as model]
            [caribou.app.controller :as controller]))
```

Now you are ready to start writing some actions!

## Controller Actions are Functions

To create a controller action is simply to write a function.  Caribou uses the
[Ring protocol](https://github.com/ring-clojure/ring) as its basis for handling
requests and returning responses.  In its simplest form, a controller looks like
this:

```clj
(defn basic-action
  [request]
  {:status 200 :body "This is a simple response"})
```

Here we are ignoring anything in the request map and simply returning a response
of 200 with the body "This is a simple response".  No fancy markup, no database
transactions, nothing.  If you have simple needs, this may be all you require. 

However, it is likely that you will want some information that lives in the
request.  That is the subject of the next section.  

## Contents of the Request Map

Living in the request map are a variety of helpful keys that provide information
about the nature of the incoming request.  This is the basic information any
controller action can use to tailor a response to that specific request.  

There are some basic keys that are available in any Ring request, currently the
following:

```clj
:uri 
:scheme 
:content-type 
:content-length 
:character-encoding 
:headers 
:request-method 
:body 
:ssl-client-cert 
:remote-addr 
:server-name 
:server-port 
```

Yet more are added by some Ring middleware that Caribou includes by default
(which you are free to remove if you wish):

```clj
:cookies 
:session 
:query-string 
:params 
:query-params 
:form-params 
:multipart-params 
```

There is a salad of params types that is an artifact of each being provided by a
separate ring middleware handler.

Then there are the keys added by Caribou.  There are some basic ones which are
provided to help with rendering:

* **:template** A function which renders the template associated to this page
    when called with a map of substitution values.
* **:page** A reference to the Page item that was matched during routing time.
* **:is-xhr** A boolean which signifies whether or not this request is xhr.
* **:route-params** A map of any parameters extracted from the url when the
    route was matched.

And then there are all the helpers.  A helper is simply a clojure function that
lives inside request map.  Caribou provides a handful of helpers by default, and
you can add any more that seem helpful.

```clj
;; value handling
:equals 

;; string handling
:truncate 
:linebreak 
:smartquote 

;; routing
:route-for 

;; image resizing
:resize 

;; date handling
:now
:ago 
:hh-mm 
:yyyy-mm-dd 
:yyyy-mm-dd-or-current 
:date-year 
:date-month 
:date-day
```

## Parameters from Routes are Available in Controllers

In order to provide something beyond our first simple action, let's use some of
the information from the incoming request.  In this example, we use a `:name`
parameter to customize our response:

```clj
(defn parameter-action
  [request]
  (let [request-name (-> request :params :name)]
    {:status 200 :body (str "Hello " request-name "!")}))
```

This way, if this action is triggered by a page associated to the route
"/hello/:name" for instance, the `:name` parameter will be set by whatever the
value of the url is in that position.  So if someone makes a request to
"/hello/lichen" the response will come back as

```
Hello lichen!
```

One basic pattern that is used over and over is to pull up some content from a
model based on the value of a parameter and use that to form the response.  An
example would be, given the route "/hello/:name" and a request to
"/hello/antler", to pull up some content from a "User" model and respond with
something that lives in that instance.  In this case we can say that the User
model has a "Greeting" field that they prefer to be greeted by that is stored in
the database:

```clj
(defn pick-action
  [request]
  (let [request-name (-> request :params :name)
        user (model/pick :user {:where {:name request-name}})
        greeting (:greeting user)] ;; this user's :greeting is "Obo"
    {:status 200 :body (str greeting " " request-name "!")}))
```

The response for this would be:

```
Obo antler!
```

## Rendering Provides Data to Templates

If you are using Caribou's default templating language,
[Antlers](https://github.com/antler/antlers), you can use the built in
`caribou.app.controller/render` method to render your templates.  It will use
the template defined in the page that routed the request to this action in the
first place.  So instead of returning a map with `:status` and `:body` in it,
you can just call render on some parameters instead.  A basic call looks like
this:

```clj
(defn pick-action
  [request]
  (let [request-name (-> request :params :name)
        user (model/pick :user {:where {:name request-name}})]
    (controller/render (assoc request :user user))))
```

The user map for this example contains:

```clj
{:greeting "Salutations" :name "Tundra Warrior"}
```

Then in a template:

```html
{{user.greeting}} {{user.name}}!
```

And out comes!:

```
Salutations Tundra Warrior!
```

Any key that is present in the map passed into `caribou.app.controller/render`
can be used inside a template, including information about the request and the
currently rendering page.  So if you need a page title and the current URL for
instance,

```html
<html>
  <head>
    <title>{{page.title}}</title>
  </head>
  <body>
    <p>You are currently visiting {{uri}}!  Welcome!</p>
  </body>
</html>
```

More information about template rendering can be found in the
[Rendering Templates](#rendering-templates) section.

## Defining a Siphon

Often in controllers, the main work is to pull some content up out of the
database based on the incoming parameters and hand that content to the template
for rendering.  This doesn't apply to actions that update or create content or
make requests of their own, but it does apply to any request that is simply
fetching data that is then presented to the requester in some meaningful way.

Along these lines, Caribou has the concept of a Siphon.  A Siphon is a
specification of what data to pull up and how to associate it based on the
parameters of an incoming request.  It is itself data that lives in a map inside
the Page:

```clj
(def page-with-siphon
  {:home 
   {:GET 
    {:controller "home" 
     :action "index" 
     :template "index.html"
     :siphons {:categories {:spec {:model :category
                                   :op    :gather
                                   :order {:created-at :desc}}}
               :user {:spec {:model :user
                             :op    :pick
                             :where {:id :$user-id}}}}}}})
```

Then you can access these values directly in the template without ever
having to build a controller!  These values live inside the page under
the `:content` key:

```html
<h1>Welcome {{page.content.user.name}}!</h1>

<p>Here are some Categories for you:</p>
<ul>
{{#page.content.categories}}
<li>{{name}}</li>
{{/page.content.categories}}
</ul>
```

## Defining Pre-Actions

Sometimes you find yourself writing the same code over and over for many
different actions.  This can be to add some information into the request or to
prevent the action from running entirely if certain conditions aren't met.
Rather than include the same block of code or call to the same function at the
beginning of every action like this, you can instead register a pre-action for
these actions.

Say your desired pre-action is simply adding something to the request map:

```clj
(defn pre-tundraize
  [action request]
  (action (assoc request :tundra "The serene open tundra")))
```

Notice pre-actions take two arguments, the action that would have originally be
called and the incoming request.  This pre-action unconditionally calls the
original action with a new `:tundra` key in the request map.  To register this
as a pre-action for a given controller action, simply call
`caribou.app.routing/register-pre-action` with the slug of the page governing
this action:

```clj
(caribou.app.routing/register-pre-action :home pre-tundraize)
```

Another use case is to prevent the action from running at all in certain cases,
for example if the request is not authorized in some way:

```clj
(defn ensure-authorized
  [action request]
  (if (authorized? request)
    (action request)
    {:status 401 :body "Not authorized!"}))
```

Then you could register this pre-action in the same way as before: 

```clj
(caribou.app.routing/register-pre-action :protected ensure-authorized)
```

Now this action will only be run if the call to `(authorized? request)` returns
true.

