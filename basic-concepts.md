# Basic Concepts

## Models and Fields

Models in Caribou are a representation of the data of your application.  Models
can be created like any content, but models are special in that creating a model
enables a new type of content to be created.  Conceptually there should be a
model for every variety of real world data that the application is capturing.
Each model has a set of Fields which represent the different types of data that
the model keeps track of.  These fields can be of a variety of types, things
like text, images, numbers, dates, and even associations to other models.

As an example, to create an application that lets you create presentations, you
could have a Presentation model that would have a "title" text field and maybe a
"preview" image field, and a Slide model with a "caption" text field and an
"image" field with an image containing the slide content.  Then, you could
create an association between Presentation and Slide so that Presentation has a
collection of Slide objects.  Once these models are created, you could start
creating Presentations and populate them with Slides.

This flexibility in defining what kind of data your application will contain
enables an endless variety of possible applications to be built.  Once a model
exists in the system, you can read data from the API or create new content in
the Admin.

Model itself is a model, with a collection of Fields (which is also a model!)
To read more, check out the [Introduction to Data
Modeling](models.html).

## Pages

Pages are the way to define how urls are matched in your application.  Each page
represents a route that can be matched when a user navigates to a particular
url.  Once a route is matched, the controller associated to that page is
triggered with the parameters defined by that route.  Pages form a nested
structure, so if a page is a child of another page, it inherits its parent's
route and adds its own unique path onto it.  In this way the routing structure
of an application can be organized hierarchically, simplifying what could
otherwise be a complicated tangle of routes.

Read more at [Defining Routes and Pages](routes.html).

## Controllers

Once a route has been matched, the corresponding controller is triggered.  A
controller in Caribou is just a Clojure function that takes a single argument,
`request`, and generates a response based on the information contained in the
request.

A controller can be thought of as the code glue that links requests to
responses.  This can be as simple as directly rendering a static template in the
most basic case to creating models, hitting remote apis, writing files, image
manipulation, or really anything that can be programmatically accomplished
(which is quite a lot these days!)

Everything you need to know about [Controllers is here](controllers.html)

## Templates

In practice, a controller can use the built in template system called
[Antlers](http://github.com/antler/antlers) to render html or json (or any other
format for that matter).  Any parameters passed into the built in `render` call
will be available in the template.

Templates are simple text files with portions which can be substituted out
dynamically.  This is the difference between static sites, which only ever
display the same thing over and over again, and a fully dynamic website which
can respond in endless ways depending on what is desired.

To see more on how this is done, check out the section on
[Rendering Templates](templates.html)

