# Introduction to Data Modeling

Defining models for an application is the heart of a Caribou project.  Once a
model is created a host of capabilities are automatically generated for that
newly created model.  This section details the means for creating new models and
expanding on existing models.

## Creating Models

Creating a model is just like creating any other content in a Caribou project.
The first step is to acquire a configuration map, which is detailed in the
[How Configuration Works in Caribou](configuring.md)
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
    reformats that string according to the [field](models.md) configuration.
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

