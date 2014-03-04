# Rendering Templates

Caribou comes with a built-in template rendering system called
[antlers](https://github.com/caribou/antlers).  When a defined page specifies a
`:template` key, then at render time it will search for the given template in
the `resources/templates` directory of your project.  If found, it will render
that template by substituting in values from the map given to render.  In this
way, the map given to render provides an environment of bindings accessible from
inside the template.

The simplest usage of `render` passes the request in directly, so that any value
in the request is accessible in the template:

```clj
(defn some-action
  [request]
  (caribou.app.controller/render request))
```

In this case the controller is doing nothing but passing the request map it
received on to the template to be rendered.  Any key in this map can be
accessed from inside a template like so:

```handlebars
    Hello template argument :tundra -- {{tundra}}
```

Now if the request map contains the value "Arctic" under the key `:tundra`,
this will render as:

```handlebars
    Hello template argument :tundra -- Arctic
```

If the map is nested, successive maps can be accessed through the '.' pattern.
So if this is the template:

```handlebars
    Hello nested template argument -- {{tundra.denizens}} !
```

And it is given a map like this:

```clj
{:tundra {:denizens "Caribou"}}
```

Then the template will render out like this:

```html
    Hello nested template argument -- Caribou !
```

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

```handlebars
    {{#lakes}}
      {{name}}
    {{/lakes}}
```

This would render as:

```txt
    Huron
    Erie
    Crater
```

But what if we want the last one to be emphasized?  This works:

```handlebars
    {{#lakes}}
      {{name}}{{#loop.last}}!!!{{/loop.last}}
    {{/lakes}}
```

```txt
    Huron
    Erie
    Crater!!!
```

Other loop variables include:

    loop.first       -->  true/false
    loop.last        -->  true/false
    loop.item        -->  the current item in the loop
    loop.index       -->  the current index
    loop.inc-index   -->  one-based index (useful for things)
    loop.count       -->  total count of items in this list
    loop.outer       -->  a reference to any loop variables from an outer loop.
                             outer can also have an outer, ad infinitum.

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

```handlebars
    {{excite "yellow"}}  -->   yellow!
```

Or with a value from the same map:

```clj
(defn some-action
  [request]
  (caribou.app.controller/render
   (assoc request
     :antler "Velvet"
     :excite (fn [s] (str s "!")))))
```

```handlebars
    {{excite antler}}  -->   Velvet!
```

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

```html
    <a href="{{route-for :somewhere {:where "yellow"} }}">somewhere yellow</a>
```

This will produce:

```html
    <a href="/place/yellow">somewhere yellow</a>
```

Of course, the value of the params can also be a value in the request map.  So
if you want the url to depend on the value of `:where` in the render map, simply
refer to that in your params map:

```html
    <a href="{{route-for :somewhere {:where where} }}">somewhere {{where}}</a>
```

Then if you pass in a map to render like this:

```clj
{:where "pink"}
```

Your template will render out as:

```html
    <a href="/place/pink">somewhere pink</a>
```

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

```html
    <img src="/{{slide.image.path}}" />
```

To resize it to have a width of 500:

```html
    <img src="{{resize slide.image {:width 500} }}" />
```

Or a height of 200 with a quality of 0.7:

```html
    <img src="{{resize slide.image {:height 200 :quality 0.7} }}" />
```

You get the idea.

## Templates can Inherit Structure from other Templates

Sometimes you have a set of templates that all share a common markup layout, and
really only differ in one content block somewhere in the middle.  This is what
template inheritance is for, which is provided in antlers in the form of blocks.

To declare a block, use the `{{%...}}` syntax, as in the following example.

Suppose you have a file "layout.html" which looks something like this:

```handlebars
    HEADER
      MONOLITHIC BODY
    FOOTER
```

But you would like to have other bodies, like `BODY OF MODULARITY`, without
replicating `HEADER` and `FOOTER` over and over again.  Here is the perfect use
case for a block:

```handlebars
    HEADER
      {{%body}}{{/body}}
    FOOTER
```

Then, in another file "modular.html" can be the content:

```handlebars
    {{< templates/layout.html}}
    {{%body}}BODY OF MODULARITY{{/body}}
```

Which, when called with `(antlers/render-file "modular.html" {})` yields:

```handlebars
    HEADER
      BODY OF MODULARITY
    FOOTER
```

Now, you can have another file called "alternate" which can have totally
different contents for the `body` block.  You only need to specify the changes
in the blocks, not the rest of the file:

```handlebars
    {{< layout.html}}
    {{%body}}This is a more conversational body for the same layout template{{/body}}
```

Which yields when rendering "alternate":

```handlebars
    HEADER
      This is a more conversational body for the same layout template
    FOOTER
```

In this way you can reuse layouts repeatedly and only need to specify what is
different.

## Swapping out the template engine

There is nothing binding you to using
[Antlers](https://github.com/caribou/antlers) as your template rendering engine.
In fact, any render function can be used if it of the right form.  A render
function takes two arguments: a template to be rendered and the map representing
the values available to the template.  Any function of this form can be passed
in under the key `:render-fn` during render time and it will be used in place of
the built in rendering engine.

```clj
(defn custom-render
  [template environment]
  (let [template-contents (read-template-somehow template)]
    (substitute-values-somehow template-contents environment)))

(defn some-action
  [request]
  (caribou.app.controller/render (assoc request :render-fn custom-render)))
```

You may even want to define your own render if you are using the same
`:render-fn` over and over again:

```clj
(defn render
  [request]
  (caribou.app.controller/render (assoc request :render-fn custom-render)))
  
(defn some-other-action
  [request]
  (render request))
```
