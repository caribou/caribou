# Data Migrations

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

## Writing a migration

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

