# Roadmap

Caribou is far from complete.  Who knows if it will ever be complete?

These are our goals for the immediate term.  Please let us know if there is
anything you consider a pressing flaw or omission and we will add it to this
list!

### Abstract persistence layer to accommodate Datomic and other NoSQL databases.

Caribou currently supports SQL databases, but there is a lot of demand for
supporting NoSQL databases as well.  This would require creating a
PersistenceLayer protocol and abstracting the current SQL persistence into being
a SQL implementation of that protocol.

### Plugin system for adding components in a structured way

Since Caribou is just Clojure code you can add any library you want by adding a
dependency to your project.clj, but there is a demand for pluggable components
that involve models, migrations, hooks, templates, code and/or javascript which
all support a given kind of functionality (like a blog, or products etc).  We
have a prototype for this and it should be available in an upcoming release.

### Data merging/synchronization between environments.

One of the most requested features is to be able to merge data from one 
environment to another.  Right now model changes made in development must be 
recreated in production (using the Admin or with a migration).  It would be 
great if there were a way to automatically push state from environment to 
environment, not just model changes but any data or content changes.  Needless 
to say this is a moderate undertaking but also one of our main goals.  We are 
currently working on an approach for this, stay tuned. 

### Full CMS functionality from the Admin.

You can do a lot of stuff with the current Admin (build models and pages, create
content).  But it is far from a streamlined content management system.  We now
have a UX person (Hi Colleen!) who is dedicated to designing a CMS that is as
usable as possible for non-technical people after a Caribou site has passed from
the hands of a developer to the hands of someone who will be updating the site
on a regular basis.  
