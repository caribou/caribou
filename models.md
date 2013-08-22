# Introduction to Data Modeling

Defining models for an application is the heart of a Caribou project.  Once a
model is created a host of capabilities are automatically generated for that
newly created model.  This section details the means for creating new models and
expanding on existing models.

## Creating Models

Creating a model is just like creating any other content in a Caribou project.
The first step is to acquire a configuration map, which is detailed in the
[How Configuration Works in Caribou](configuring.html)
section.  

Assuming a configuration exists and it is called `config`, a model can be
created from the repl with the following call:

```clj
(caribou.core/with-caribou config 
  (caribou.model/create 
   :model
   {:name "Presentation"
    :fields [{:name "Title" :type "string"}
             {:name "Preview" :type "asset"}]}))
```

Some things to note about this code:

* The first line calls `caribou.core/with-caribou` with an existing
configuration map.  This configuration map among other things contains
information about the database connection.  Since this call is creating a new
model, this will actually generate a new table for that model inside whatever
database is referred to by the given configuration map under its `:database`
 key.  This means of configuration means that you can create models in different
databases just by swapping out the configuration map.  For clarity, from here on
out we will assume the config map is provided.

* The next line calls the fundamental function `caribou.model/create`.  This
call is used to create any content inside of a Caribou project, and corresponds
to inserting a new row in the database given by the configuration map.

* The next line contains only the key `:model`, and signifies that we are
creating a model, as opposed to any other content type currently known to the
system.  Once a model is created (in this case the Presentation model), content
of that variety can be created using the same call, but swapping out the key
here (which for the case of Presentations, would be `:presentation`).  If a call
to `caribou.model/create` is made with a key that does not represent a current
model known to the system this will throw an exception.

* Next comes a map of properties that define the model being created.  This list
of properties has a key for each Field in the Model model.  Given a different
model, the available keys in this map would be different.

* Ultimately, the definition of a model really depends on the fields in that
model.  In this case, two custom fields are created for the Presentation model,
a Title of type "string", and a Preview of type "asset".  Once this model
exists, new Presentations can be created that have titles and previews in the
same manner:

```clj
(caribou.model/create 
 :presentation
 {:title "Caribou!"
  :preview {:source "path/to/preview/image.png"}}))
```

In this way, creating a model allows new kinds of content to be created.
Everything else in Caribou flows from this basic idea.

## Field Types

There are a number of different field type models can have.  Here is a summary:

* **address** - Store a location as a set of fields or lat/lng pairs.
* **asset** - Represents any kind of file, including images.
* **boolean** - Represent a single true/false value.
* **decimal** - Store a single decimal value of arbitrary precision.
* **enum** - Represent a finite set of possible values.
* **integer** - A single number with no decimal digits.
* **password** - Store an encrypted value that can be matched but not read.
* **position** - A value that automatically increments when new content is added.
* **slug** - A string that depends on some other string field for its value, and
    reformats that string according to the [field](models.html) configuration.
* **string** - The workhorse.  Represents a single short string.
* **structure** - Stores arbitrary clojure data structures in EDN format.  
* **text** - Used to store arbitrarily long text.  
* **timestamp** - Represents dates and times of all varieties.

## Associations

Beyond the simple field types, much of the richness of a Caribou model structure
lies in the associations that are created between models in the system.  Model
and Field have this relationship, where Model has a "collection" of Fields and
Fields are a "part" of Model.  This provides a one to many relationship between
the Model model and the Field model.

Every association in Caribou is represented by a field in the corresponding
models, which means that there is an association field in each model
representing the two sides of the association.  This means each association type
has a reciprocal type, and that every association has one and only one
reciprocal association field that lives in another model somewhere.

The different types of associations available in Caribou are:

* **collection** - This association field type represents a collection of
    things, meaning there are potentially many pieces of content associated to
    any content of this model type.  The reciprocal type of association is the
    "part".
    
* **part** - The reciprocal to "collection", this means that any content of this
    model variety will potentially belong to content of the model that it is a
    "part" of.  Any content that is part of another collection cannot belong to
    another collection.
    
* **link** - The link association type is its own reciprocal, and represents a
    many to many relationship to another model.  This behaves just like a
    collection except that the associated content can have many associations as
    well.

## Creating and Updating Content

As detailed before at the end of [Creating Models](models.html), once a
model has been created, new content can be created according to that model.

```clj
(caribou.model/create 
 :model
 {:name "Presentation"
  :fields [{:name "Title" :type "string"}
           {:name "Preview" :type "asset"}]}))

(def caribou-presentation
  (caribou.model/create 
   :presentation
   {:title "Caribou!"
    :preview {:source "path/to/preview/image.png"}})))
```

The first call to `caribou.model/create` creates the Presentation *model*, and
the second creates new Presentation *content*.  Notice the fields defined during
model creation are available during content creation time.  Next, let's create a
new Slide model and associate it to Presentation:

```clj
(caribou.model/create 
 :model
 {:name "Slide"
  :fields [{:name "Image" :type "asset"}
           {:name "Caption" :type "string"}
           {:name "Presentation" :type "part"
            :target-id (caribou.config/draw :models :presentation :id)}]}))
```

The key here is that we made a new field called "Presentation" of type "part".
In order to associate this new field to the Presentation model, we need the id
of the Presentation model, which lives inside the current Caribou config.  It
can be accessed using the `caribou.config/draw` method, which indexes anywhere
inside the currently applied configuration map.  In this case, we need only the
`:id`, which is passed in as the new association field's `:target-id`.

Since the new "Presentation" field inside the Slide model is of type "part", a
reciprocal "collection" association is automatically created inside of the
Presentation model.  Now, Slides can be created and associated to Presentations:

```clj
(def first-slide
  (caribou.model/create 
   :slide
   {:caption "Welcome to Caribou!"
    :image {:source "welcome/to/caribou.jpg"}
    :presentation caribou-presentation})))
```

Since Presentation has a collection of Slides, you can also create Slides in the
context of a given Presentation using `caribou.model/update`:

```clj
(caribou.model/update
 :presentation 
 (:id caribou-presentation)
 {:title "Caribou Redux!"
  :slides [{:caption "Explaining Caribou Models"
            :image {:source "explaining/caribou/models.jpg"}}
           {:caption "How to Update a Caribou Model"
            :image {:source "updating/caribou/models.jpg"}}]}))
```

This creates two new Slides and associates them to the given presentation.  A
couple things to note about this update:

* `caribou.model/update` requires an additional parameter which is the `:id` of
  the preexisting content item you wish to update.  This is automatically
  generated when a content item is first created, so is present in the map that
  is returned from the original call to `caribou.model/create` that created that
  content item (above this was stored under the var `caribou-presentation`).

* To add items into the collection, we provide a vector of maps under the
  `:slides` key in the update.  This works just as well for create.  Each map in
  the collection vector will be created and associated to the given object.  In
  fact, this is how we created the model originally, since `:fields` is a
  collection that lives in the Model model.  If one of these maps contains an
  `:id`, it will find the associated item with the given id and update it rather
  than creating a new one.

## Default Model Fields

There are a number of default fields that are added to a model automatically.
These play various roles in managing the content internally, and also provide
some handy features that all content is likely to need.  These fields are:

* **:id** -- The `:id` represents a unique integer identifier that is used
    throughout Caribou.  Every content item in Caribou is given an `:id`, and
    all content can be retrieved based on its model type and its `:id`.  This is
    also the mechanism under the scenes that tracks how different items are
    associated to one another.  `:id` always increments starting from `1`, so
    every item obtains a unique `:id` within its model table.

* **:position** -- The `:position` field allows content to be ordered in an
    arbitrary fasion.  Without the `:position` field we would be stuck
    retrieving content only by name, or id or title or something.  `:position`
    allows people to order content exactly how it should appear.  Without
    outside intervention, `:position` increments automatically starting from
    `1`, just like `:id`.  `:position` however can change, whereas once an `:id`
    is acquired it is invariant for the lifetime of the application.

* **:locked** -- This boolean field, if `true`, prevents the given content item
    from being modified by a `caribou.model/update` call.  This is handy to
    protect the built in model fields from arbitrary changes which could
    undermine the very functioning of Caribou itself.  That is not to say built
    in models are unchangeable: new fields can be added to any model.  But
    someone cannot remove the "Name" field from a model, for instance.  Caribou
    needs this field to run.  Probably you will not need to set this field
    yourself, but you could have a vital content item that plays a similar role
    in the application as a whole, in which case setting it to `locked` will
    safeguard that content from changing out from under you.

* **:created-at** -- This is a timestamp that is set automatically when a piece
    of content is created.  This way you always know when something was created!

* **:updated-at** -- This is another timestamp, but it gets set every time
    something is updated.  Can be useful to order by this if you always want the
    most recent content (or least recent!)

## Retrieving Content

Once models and content have been created, the ideal thing would be to be able
to retrieve it again!  This capability is provided by the `caribou.model/gather`
and `caribou.model/pick` functions.

To retrieve all Presentations in the system, we just gather them:

```clj
(def all-presentations
  (caribou.model/gather :presentation))
  
--> [{:id 1 :title "Caribou Redux!" :preview {...} ...}] ;; a lot of information not shown here
```

`caribou.model/pick` is just like gather, except it only returns a single item:

```clj
(def first-presentation
  (caribou.model/pick :presentation))

--> {:id 1 :title "Caribou Redux!" :preview {...} ...}
```

Without arguments, `pick` will return the first item, and `gather` will return
all items.  To refine our results, an options map can be passed in as the second
argument:

```clj
(def all-presentations
  (caribou.model/gather
   :presentation
   {:where {:title "Caribou Redux!"}}))
  
--> [{:id 1 :title "Caribou Redux!" :preview {...} ...}]
```

This map presents one of the features of a gather map, `:where`.  The full list is:

* **:where** -- present conditions which narrow and refine the results.
* **:include** -- fetch associated content along with the primary results.
* **:order** -- order the gathered results based on given criteria.
* **:limit** -- limit primary results to a certain number.
* **:offset** -- index into results by the given offset.

Let's take a look at these one by one.

### **:where**

One of the great sources of power for the gather call is that the `:where` map
can express conditions across associations:

```clj
(def redux-slides
  (caribou.model/gather
   :slide
   {:where {:presentation {:title "Caribou Redux!"}}}))
  
--> [{:id 1 :caption "Welcome to Caribou!" ...}
     {:id 2 :caption "Explaining Caribou Models" ...} 
     {:id 3 :caption "How to Update a Caribou Model" ...}]
```

The point here is that we are gathering slides based on a condition that exists
on the associated Presentation item.  This is cool.

You can also have parallel conditions.  This acts like a logical "AND":

```clj
(def redux-slides
  (caribou.model/gather
   :slide
   {:where {:presentation {:title "Caribou Redux!"}
            :id {:>= 2}}}))
  
--> [{:id 2 :caption "Explaining Caribou Models" ...} 
     {:id 3 :caption "How to Update a Caribou Model" ...}]
```

### **:include**

One thing you will notice right away when gathering content is that though
associations exist, associated items do not come through the regular
`caribou.model/gather` call by default.  This is what the `:include` map is for.
The `:include` map defines a nested set of association field names that trigger
the retrieval of associated content.

```clj
(def redux-and-slides
  (caribou.model/pick
   :presentation
   {:where {:title "Caribou Redux!"}
    :include {:slides {}}}))
  
--> {:id 1 
     :title "Caribou Redux!" 
     :preview {...}
     :slides [{:id 1 :caption "Welcome to Caribou!" ...}
              {:id 2 :caption "Explaining Caribou Models" ...} 
              {:id 3 :caption "How to Update a Caribou Model" ...}]}
```

The `:include` map can travel arbitrarily deep along the model association
graph, so if Slide had a collection of another model, say "Paragraphs", then you
could retrieve those as well with another level of the `:include` map:

```clj
(def redux-slides-and-paragraphs
  (caribou.model/pick
   :presentation
   {:where {:title "Caribou Redux!"}
    :include {:slides {:paragraphs {}}}}))
  
--> {:id 1 
     :title "Caribou Redux!" 
     :preview {...}
     :slides [{:id 1 :caption "Welcome to Caribou!" :paragraphs [...] ...}
              {:id 2 :caption "Explaining Caribou Models" :paragraphs [...] ...} 
              {:id 3 :caption "How to Update a Caribou Model" :paragraphs [...] ...}]}
```

You can also perform parallel includes, so if a Presentation also had an
association to an existing "Person" model called "Authors", you could retrieve
the Presentation, all its Slides and their Paragraphs, and the Authors of the
Presentation all in one gather call:

```clj
(def redux-authors-and-slide-paragraphs
  (caribou.model/pick
   :presentation
   {:where {:title "Caribou Redux!"}
    :include {:authors {}
              :slides {:paragraphs {}}}}))
  
--> {:id 1 
     :title "Caribou Redux!" 
     :preview {...}
     :authors [{:name "Donner"} {:name "Blitzen"} ...]
     :slides [{:id 1 :caption "Welcome to Caribou!" :paragraphs [...] ...}
              {:id 2 :caption "Explaining Caribou Models" :paragraphs [...] ...} 
              {:id 3 :caption "How to Update a Caribou Model" :paragraphs [...] ...}]}
```

Obviously this can get out of control, and it wouldn't be hard to pull in every
content item in the site in a single call.  Any single gather call can be broken
into individual gathers that fetch the content when needed.

### **:order**

The `:order` map is used to control the order of the returned items.  By
default, content is ordered based on that model's `:position` field, but any
order can be used.  Here is an example of ordering by `:updated-at`:

```clj
(def redux-slides-ordered-by-updated-at
  (caribou.model/gather
   :slide
   {:where {:presentation {:title "Caribou Redux!"}}
    :order {:updated-at :desc}}))
  
--> [{:id 3 :caption "How to Update a Caribou Model" :updated-at #inst "2013-06-21T22:37:35.883000000-00:00" ...}
     {:id 2 :caption "Explaining Caribou Models" :updated-at #inst "2013-06-21T22:37:34.883000000-00:00" ...} 
     {:id 1 :caption "Welcome to Caribou!" :updated-at #inst "2013-06-21T22:37:33.883000000-00:00" ...}]
```

The value for the property being ordered can be either `:asc` or `:desc`,
representing ascending or descending respectively.

The `:order` map, like the `:where` and `:include` map, can propagate across
associations, and order across many properties simultaneously:

```clj
(def redux-slides-parallel-ordering
  (caribou.model/gather
   :slide
   {:order {:updated-at :desc
            :id :asc
            :presentation {:title :desc}}}))
  
--> [{:id 3 :caption "How to Update a Caribou Model" :updated-at #inst "2013-06-21T22:37:35.883000000-00:00" ...}
     {:id 1 :caption "Welcome to Caribou!" :updated-at #inst "2013-06-21T22:37:34.883000000-00:00" ...}
     {:id 2 :caption "Explaining Caribou Models" :updated-at #inst "2013-06-21T22:37:34.883000000-00:00" ...}]
```

### **:limit**

The `:limit` option specifies a maximum number of items to retrieve, in the case
that there are more items than you wish to handle at any given time:

```clj
(def redux-slides-limited
  (caribou.model/gather
   :slide
   {:order {:updated-at :desc
            :id :asc
            :presentation {:title :desc}}
    :limit 2}))
  
--> [{:id 3 :caption "How to Update a Caribou Model" :updated-at #inst "2013-06-21T22:37:35.883000000-00:00" ...}
     {:id 1 :caption "Welcome to Caribou!" :updated-at #inst "2013-06-21T22:37:34.883000000-00:00" ...}]
```

One thing to note: only the outermost model is limited.  Any items included
across associations will not be limited.  Keep this in mind if you have items
with a large number of associated items in a collection or link.  In that case
it is better to not include the content directly, but rather to make an
additional gather on associated items once the outer item is retrieved:

```clj
(let [presentation   (caribou.model/pick
                      :presentation
                      {:where {:title "Caribou Redux!"}})
      limited-slides (caribou.model/gather
                      :slide
                      {:where {:presentation {:id (:id presentation)}}
                       :limit 2})]
  (assoc presentation :slides limited-slides))
```

### **:offset**

`:offset` is used in conjunction with `:limit`.  It finds subsequent sets of
content given whatever would be returned from the gather normally, but has been
excluded through the use of a `:limit`.

```clj
(def redux-slides-limited-and-offset
  (caribou.model/gather
   :slide
   {:order {:updated-at :desc
            :id :asc
            :presentation {:title :desc}}
    :limit 2
    :offset 1}))
  
--> [{:id 1 :caption "Welcome to Caribou!" :updated-at #inst "2013-06-21T22:37:34.883000000-00:00" ...}
     {:id 2 :caption "Explaining Caribou Models" :updated-at #inst "2013-06-21T22:37:34.883000000-00:00" ...}]
```

This can be used to implement pagination, for example.

## Data Migrations

When making model changes (which ultimately change the schema of the tables you
are working with), it is wise to implement them as migrations so that you can
recreate your schema in any database environment you will eventually encounter.
This makes it easy to switch databases or even database libraries and continue
to use your existing code.

To create a migration, there are three steps:

* Writing the migration
* Specifying the order of the migration
* Running the migration

Let's look at the first of these tasks.

### Writing a migration

First, create a new namespace inside your `src/{project}/migrations/` directory.
It should contain two functions, `migrate` and `rollback`:

```clj
(ns taiga.migrations.example
  (:require [caribou.model :as model]))
  
(defn migrate
  []
  (model/create
    :model
    {:name "Example"
     :fields [{:name "Content" :type "string"}]}))
     
(defn rollback
  [])
```

Once the migration exists, you have to add it into the order vector that keeps
track of migration order.  This is required because migrations are not
necessarily commutative: certain migrations must have already run before others
can make sense.  This is most apparent for a migration that adds a field to an
existing model: if the model hasn't been created yet, adding a field to it will
fail!

Every project has an `{project}.migrations.order` namespace in the
`src/{project}/migrations/order.clj` file.  Open this and add your migration to
the list somewhere it makes sense:

```clj
(def order ["default" "admin" "example"]) ;; <--- Here for example!
```

Once order has been instated, time to run the migration:

```
% lein caribou migrate resources/config/development.clj
```

The `lein caribou migrate` command accepts the path to a config file because it
needs to know what database to run the migration on.  This is helpful if you
have many different environments each with their own database (that may or may
not live on this local machine).  `lein caribou migrate` keeps track of which
migrations have already been run in that database and ensures each migration
only runs once and in the order specified in your `{project}.migrations.order`
namespace.

## Content Localization

Localizing content in Caribou means providing different values for the fields in
a content item depending on what "locale" the application is receiving requests
from.  Localization of a Model is done on a field by field basis.  This means
that even what items are associated to what can be localized if desired.

To begin, let's create a model that will hold content that varies between
locales (consider this example to be entirely contrived):

```clj
(caribou.model/create 
  :model 
  {:name "Wisdom" 
   :fields [{:name "quotation" 
             :type "string" 
             :localized true}]})
```

Notice the line `:localized true`.  This signifies that values stored in this
field will have different values based on which locale is being requested.

Next, let's create a new locale.  Because this is a tutorial, we will create a
locale for Klingon (complete with utterly fabricated locale code):

```clj
(caribou.model/create 
  :locale 
  {:language "Klingon" 
   :region "Qo'noS" 
   :code "ql-QN"})
```

These are the three required fields for created a locale.  Notice that creating
a locale is exactly the same as creating any other content in Caribou.  Locale
is a model.  Everything is a model.  Even Model is a model.

Next, let's create a new instance of our new Wisdom model.  This is easy, we
know how to do this:

```clj
(caribou.model/create
  :wisdom
  {:quotation "Trust, but verify"})
```

To get the basic instance back, we can call gather on the Wisdom model:

```clj
(caribou.model/gather :wisdom)

---> ({:id 1 :quotation "Trust, but verify" ...})
```

But the whole point is to pull the content for our new Klingon locale, "ql-QN".
To do this, we simply specify the locale code in the gather:

```clj
(caribou.model/gather :wisdom {:locale "ql-QN"})

---> ({:id 1 :quotation "Trust, but verify" ...})
```

This is great, but it still has the same value.  This is because we haven't
specified what the localized value should be.  To do that, let's call
`caribou.model/update` with the right locale:

```clj
(caribou.model/update 
  :wisdom
  1
  {:quotation "yIvoq 'ach yI'ol"}
  {:locale "ql-QN"})
```

Notice how `update` takes a second map.  The first map is only for specifying
what values the content has, while the second is full of modifiers and options
that won't actually be directly committed as values for this instance.

Now when we do our gather, we get the right values:

```clj
(caribou.model/gather :wisdom {:locale "ql-QN"})

---> ({:id 1 :quotation "yIvoq 'ach yI'ol" ...})
```

Whereas the original non-localized version still exists:

```clj
(caribou.model/gather :wisdom)

---> ({:id 1 :quotation "Trust, but verify" ...})
```

This non-localized version is actually part of the "global" locale, which is
always present.  The "global" locale also supplies values for instances that don't
have a value in the localized field.  So until a specific value is given to the
`quotation` for the "ql-QN" locale, it will inherit the value that exists in
"global".  This allows you to just override the content that needs to be
overridden and provide, for instance, the same image in all locales except the
specific ones that need their own image.

