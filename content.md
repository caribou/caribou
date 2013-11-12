# Creating and Updating Content

As detailed before at the end of [Creating Models](models.md), once a
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

# Retrieving Content

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

## **:where**

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
(def redux-slides-with-id-greater-than-or-equal-to-two
  (caribou.model/gather
   :slide
   {:where {:presentation {:title "Caribou Redux!"}
            :id {:>= 2}}}))
  
--> [{:id 2 :caption "Explaining Caribou Models" ...}
     {:id 3 :caption "How to Update a Caribou Model" ...}]
```

There are also means to express more complex logical queries.  These are
available as the `'and`, `'or` and `'not` symbols inside a where map:

```clj
(def redux-slides-with-id-not-equal-to-two
  (caribou.model/gather
   :slide
   {:where {'and [{:presentation {:title "Caribou Redux!"}}
                  {'not {:id 2}}]}}))
  
--> [{:id 1 :caption "Welcome to Caribou!" ...}
     {:id 3 :caption "How to Update a Caribou Model" ...}]
```

Notice the `'and` operator takes a vector of subsequent conditions.  These
conditions take the same form as a regular where map and can be nested
recursively to provide arbitrarily complex logical predicates.  `'or` works the
same way:

```clj
(def redux-slides-welcome-or-id-of-two
  (caribou.model/gather
   :slide
   {:where {'and [{:presentation {:title "Caribou Redux!"}}
                  {'or [{:caption "Welcome to Caribou!"}
                        {:id 2}]}]}}))
  
--> [{:id 1 :caption "Welcome to Caribou!" ...}
     {:id 2 :caption "Explaining Caribou Models" ...}]
```

The other where condition facility available is the ability to do "IN" queries.
This is accomplished by providing a vector of values for a given field rather
than just a single value:

```clj
(def redux-slides-id-in
  (caribou.model/gather
   :slide
   {:where {:id [2 3]}}))
  
--> [{:id 2 :caption "Explaining Caribou Models" ...}
     {:id 3 :caption "How to Update a Caribou Model" ...}]
```

## **:include**

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

## **:order**

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

## **:limit**

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

## **:offset**

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

## **:fields**

You can ask for the query to return only certain given fields available in a
model.  This is helpful if you just need certain information from a model rather
than every value that lives in that row:

```clj
(def redux-slides-limited-and-offset-only-id
  (caribou.model/gather
   :slide
   {:order {:updated-at :desc
            :id :asc
            :presentation {:title :desc}}
    :limit 2
    :offset 1
    :fields [:id]}))

--> [{:id 1} {:id 2}]

```

You can even specify fields from included associations using this method:

```clj
(def redux-authors-and-slides-limit-fields
  (caribou.model/pick
   :presentation
   {:where {:title "Caribou Redux!"}
    :fields [:title {:slides [:caption] :authors [:name]}]
    :include {:authors {}
              :slides {}}}))
  
--> {:id 1 
     :title "Caribou Redux!" 
     :preview {...}
     :authors [{:name "Donner"} {:name "Blitzen"} ...]
     :slides [{:caption "Welcome to Caribou!"}
              {:caption "Explaining Caribou Models"} 
              {:caption "How to Update a Caribou Model"}]}
```
