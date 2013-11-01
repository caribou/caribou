# Components of a Caribou Project

Caribou is not a single library, but an ecosystem of interacting components,
each of which has the ability to stand on its own.  This idea lends a quality of
composability to the Caribou world.  If some capability does not
exist, it can be created on its own and then easily linked into a working
Caribou instance.

That said, there are some core components that lay the foundation for everything
that follows.

## Base Libraries

### Core

[Caribou Core](http://github.com/caribou/caribou-core) lays the foundation for
all of the other libraries by capturing the data model of a site as data!  A
data model is traditionally only an implicitly defined being, existing as a
conglomeration of migrations and tables whose relationships are only formed
through happy accidents within application code. Caribou Core structures the
data model in a way that allows it to serve a variety of other purposes,
including the construction of queries that filter and order based on the
relationships between models.

[Caribou Core](https://github.com/caribou/caribou-core) can be used on its own if
all you need is the dynamic models as an interface to a database backend.  In
practice it is usually supporting a site running the Caribou Frontend, Admin and
API, but nothing is stopping you from using it independently of an HTTP Ring
server.

The heart of Core is the Model system, which abstracts over a database schema
and provides methods for making schema transformations through transformations
on pure clojure data.  In every way, Model (which represents a database table)
itself is a Model, with an association to Fields (which represent the columns of
a database table) which is also a Model.  This is a radical choice, and lays the
foundation for the rest of Caribou's power.  The ability to treat Models and
Fields themselves as data enables Caribou to generate an Admin and API for you
automatically, and countless other benefits that you will discover as you go
deeper into the Caribou ecosystem.

### Frontend

[Caribou Frontend](https://github.com/caribou/caribou-frontend) uses Core as the
data layer foundation and is built on the [Clojure Ring protocol](https://github.com/ring-clojure/ring).
Ring is a flexible HTTP protocol for Clojure that abstracts over the HTTP
request and response lifecycle, turning them into plain Clojure maps.  In
practice this is an extremely powerful way to compose handlers and functionality
into a robust web server.

Frontend adds onto the Core and Ring base a fully-functional routing, controller
and rendering system.  If Core is the M, then Frontend is the VC.  At the heart
of this system is the Page, which associates routes to the controllers that run
when they are matched and the templates that are ultimately rendered with data
retrieved and defined in the controllers.

### Admin

[Caribou Admin](https://github.com/caribou/caribou-admin) provides a
browser-based interface to all of the Caribou functionality.  Things you would
previously need someone to code for you can be done with the click of a button.
Adding new Models, adding new Fields to those Models, creating content based on
those Models, adding Pages for routing and rendering, localizing content for
many languages and locales, adding Accounts and managing Permissions, all of
this is accessible through the Admin interface.  No need to build a custom admin
for every project!  This alone cuts down on the development time of a project by
a large degree, and is one of the huge advantages of using Caribou to build your
site.

### API

[Caribou API](http://github.com/caribou/caribou-api) provides a RESTful API in a
variety of formats (json, xml or csv) which tap into any content you create in
Caribou.  Create a new Model and instantly an endpoint representing that Model
is available.  Add some content for that Model, the content magically appears in
the API results.  Use any of the options for filtering and selecting content as
URL parameters that would previously only be available programmatically.  The
API again is a tangible upshot of Caribou's Model-as-Data approach.

## Peripheral Libraries

Besides the base libraries, there is a whole tundra of associated libraries that
exist on their own, but also contribute to the Caribou ecosystem.

### Lichen

[Lichen](http://github.com/caribou/lichen) is a standalone image resizing library
that enables Caribou to define image sizes during template creation.  Lichen
creates the newly resized version of the image the first time it is requested,
then reuses the cached version on each subsequent request transparently, so the
developer never needs to worry about it.  Declare what you want, Lichen worries
about how to most efficiently perform the task.

### Antlers

[Antlers](http://github.com/caribou/antlers) is a templating library that grew
out of the raw Mustache spec, but adapted to ever-expanding demands from day-to-day
use.  Today it is a fully functional templating system with blocks, helper
functions, loop variables and a host of other practical features that make it
the cornerstone of rendering in Caribou.  Of course, nothing is stopping you
from using whatever template system you want, but if you need it, it's there.

