# Rendering Templates

Caribou comes with a built-in template rendering system called
[antlers](https://github.com/antler/antlers).  When a defined page specifies a
`:template` key, it will search for the given template in the
`resources/templates` directory of your project and associate a function to
render that template into the incoming request in a controller action under the
`:template` key.  When that action calls `caribou.app.controller/render`, it
will look for this function under the `:template` key and pass it the map
`render` receives as its main argument.  This is reflected in the canonical
controller action pattern of usage:

```clj
(defn some-action
  [request]
  (caribou.app.controller/render request))
```

In this case the controller is doing nothing but passing the request map it
received on to the template to be rendered.  Inside `request` lives a key
`:template` which is a function taking a single argument: the map of data
available to the template during render time.  Any key in this map can be
accessed from inside a template like so:

    Hello template argument :tundra -- {{tundra}}

Now if the request map contains the value "Arctic" under the key `:tundra`,
this will render as:

    Hello template argument :tundra -- Arctic

If the map is nested, successive maps can be accessed through the '.' pattern.
So if this is the template:

    Hello nested template argument -- {{tundra.denizens}} !

And it is given a map like this:

```clj
{:tundra {:denizens "Caribou"}}
```

Then the template will render out like this:

    Hello nested template argument -- Caribou !

## Using Loops with Sequences from the Render Map

Any sequence of items (list or vector) in the render map can be looped over
inside a template.  

```clj
(defn find-lakes
  [request]
  (let [lakes (model/gather :lake)]
    (render (assoc request :lakes lakes))))

;; now the request map looks something like this:
{:lakes [{:name "Huron"} 
         {:name "Erie"} 
         {:name "Crater"}]}
```

Traversing a loop is simple.  In the "lake" template:

    {{#lakes}}
      {{name}}
    {{/lakes}}

This would render as: 

    Huron 
    Erie 
    Crater

But what if we want the last one to be emphasized?  This works:

    {{#lakes}}
      {{name}}{{#loop.last}}!!!{{/loop.last}}
    {{/lakes}}


    Huron 
    Erie 
    Crater!!!

Other loop variables include:

    loop.first       -->  true/false
    loop.last        -->  true/false
    loop.item        -->  the current item in the loop
    loop.index       -->  the current index
    loop.inc-index   -->  one-based index (useful for things)
    loop.count       -->  total count of items in this list
    loop.outer       -->  a reference to any loop variables from an outer loop.  outer can also have an outer, ad infinitum.

## Template Helpers

Template helpers are simply functions which live in the render map.  They are
easy to invoke, and accept arguments which can be literals or other values from
the render map:

```clj
(defn some-action
  [request]
  (caribou.app.controller/render 
   (assoc request :excite (fn [s] (str s "!")))))
```

Then in the template:

    {{excite "yellow"}}  -->   yellow!

Or with a value from the same map:

```clj
(defn some-action
  [request]
  (caribou.app.controller/render 
   (assoc request 
     :antler "Velvet"
     :excite (fn [s] (str s "!")))))
```

    {{excite antler}}  -->   Velvet!

## Existing Helpers

There are many helpers Caribou provides by default, but the two most important
are `route-for` and `resize`.  These are explained here.

* **route-for**

`route-for` is a way to generate a url based on a key and some parameters.  This
is a helpful alternative to simply hard-coding urls throughout your templates,
since it means that you are free to change your routes at will and all of the
urls in your templates will immediately reflect this.

`route-for` takes the key for a route and a map of params to be substituted into
the variable parts of the route.  So say you have a route defined like this:

```clj
["/place/:where" :somewhere []]
```

Then you need to link to this route in a template somewhere.  To generate the
url using `route-for`, in your template:

    <a href="{{route-for :somewhere {:where "yellow"} }}">somewhere yellow</a>

This will produce:

    <a href="/place/yellow">somewhere yellow</a>

Of course, the value of the params can also be a value in the request map.  So
if you want the url to depend on the value of `:where` in the render map, simply
refer to that in your params map:

    <a href="{{route-for :somewhere {:where where} }}">somewhere {{where}}</a>

Then if you pass in a map to render like this:

```clj
{:where "pink"}
```

Your template will render out as:

    <a href="/place/pink">somewhere pink</a>

* **resize**

`resize` takes existing images and resizes them to dimensions given by `:width`
and `:height` parameters.  If only `:width` or only `:height` is supplied, it
scales the image to maintain the aspect ratio of the original image.  There is
also a `:quality` option that governs the image quality of the resized image.

The first argument to `resize` is an image map, which can be obtained from a
model containing an "asset" field.  So if you have a Slide model with an "image"
field of type "asset", the resize call would work like the following.

In the controller:

```clj
(defn display-slide
  [request]
  (let [slide (model/pick :slide {:where {:slug (-> request :params :slide)}})]
    (render (assoc request :slide slide))))
```

To render the image at the original size:

    <img src="/{{slide.image.path}}" />

To resize it to have a width of 500:

    <img src="{{resize slide.image {:width 500} }}" />

Or a height of 200 with a quality of 0.7:

    <img src="{{resize slide.image {:height 200 :quality 0.7} }}" />

You get the idea.

## Templates can Inherit Structure from other Templates

Sometimes you have a set of templates that all share a common markup layout, and
really only differ in one content block somewhere in the middle.  This is what
template inheritance is for, which is provided in antlers in the form of blocks.

To declare a block, use the `{{%...}}` syntax, as in the following example.

Suppose you have a file "layout.html" which looks something like this:

    HEADER
      MONOLITHIC BODY
    FOOTER

But you would like to have other bodies, like `BODY OF MODULARITY`, without
replicating `HEADER` and `FOOTER` over and over again.  Here is the perfect use
case for a block:

    HEADER
      {{%body}}{{/body}}
    FOOTER

Then, in another file "modular.html" can be the content:

    {{< templates/layout.html}}
    {{%body}}BODY OF MODULARITY{{/body}}

Which, when called with `(antlers/render-file "modular.html" {})` yields:

    HEADER
      BODY OF MODULARITY
    FOOTER

Now, you can have another file called "alternate" which can have totally
different contents for the `body` block.  You only need to specify the changes
in the blocks, not the rest of the file:

    {{< layout.html}}
    {{%body}}This is a more conversational body for the same layout template{{/body}}

Which yields when rendering "alternate":

    HEADER
      This is a more conversational body for the same layout template
    FOOTER

In this way you can reuse layouts repeatedly and only need to specify what is
different.

## Swapping out the template engine

There is nothing special about the function that lives under the `:template` key
passed into `caribou.app.controller/render` besides the fact that it takes a map
of values as an argument and produces a string representing a rendered template.  

If you want to use a different template engine simply swap out the function
living under `:template` with your own, as long as you can wrap it into the same
form:

```clj
(defn some-action
  [request]
  (let [template (fn [render-values] (my-template-engine/render "where.html" render-values))]
    (caribou.app.controller/render (assoc request :template template))))
```

