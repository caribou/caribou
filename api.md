# Using the API

The API is already running when you fire up a Caribou project.  Its function is
to make available all the content in your system as json, xml or csv.  Every
time a model is created, a corresponding API for that model is instantly
available.

## All Content is Accessible from the API

Once a new model is created, any instances of that model can be accessed at a
URL of the form:

    http://localhost:33333/_api/{model-name}

So, to access the Model API, simply navigate to
http://localhost:33333/_api/model .  You will see a json representation of every
model in the system.  If you want a specific representation, add it as a file
extension to this basic URL structure:

    http://localhost:33333/_api/model.json
    http://localhost:33333/_api/model.xml
    http://localhost:33333/_api/model.csv

## Options in the API

All the options you would pass into a `caribou.model/gather` are available in
the API.  Add any additional constraints as query parameters to refine your
selection:

    http://localhost:33333/_api/model?include=fields&limit=2

## Changing the API root or removing the API

The routes for the API are added in your `{project}.core` namespace in the call
to `{project}.core/reload-pages`.  It will look something like this:

```clj
(defn reload-pages
  []
  (pages/add-page-routes
   admin-routes/admin-routes
   'caribou.admin.controllers
   "/_admin"
   admin-core/admin-wrapper)

  (pages/add-page-routes
   api-routes/api-routes
   'caribou.api.controllers
   "/_api"
   api-core/api-wrapper)

  (pages/add-page-routes
   (pages/all-pages)
   (config/draw :controller :namespace)))
```

As you can see, the Admin is loaded first, then the API.  If you want to change
where the API is located, just change the string "/_api" to your desired API
root.  And if you don't want the API enabled at all, simply remove the whole
`pages/add-page-routes` call to add the API routes.

