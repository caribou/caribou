# Roadmap

Caribou is far from complete.  Who knows if it will ever be complete?

These are our goals for the immediate term.  Please let us know if there is
anything you consider a pressing flaw or omission and we will add it to this
list.

* Abstract persistence layer to accommodate Datomic and other NoSQL databases.

Caribou currently supports SQL databases, but there is a lot of demand for
supporting NoSQL databases as well.  This would require creating a
PersistenceLayer protocol and abstracting the current SQL persistence into being
a SQL implementation of that protocol.

* Synchronization of state between instances using [Avout](http://avout.io/).

Right now if you use Caribou in a clustered or distributed environment and 
change a model or page, those changes won't be reflected in the other instances. 
This should be a straightforward application of Avout, so we will support this 
soon.

* Data merging/synchronization between environments.

One of the most requested features is to be able to merge data from one 
environment to another.  Right now model changes made in development must be 
recreated in production (using the Admin or with a migration).  It would be 
great if there were a way to automatically push state from environment to 
environment, not just model changes but any data or content changes.  Needless 
to say this is a moderate undertaking but also one of our main goals.  We are 
currently working on an approach for this, stay tuned. 
