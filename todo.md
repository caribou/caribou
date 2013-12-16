# TODO

- Add .gitkeep to /app

- Remove immutant dependency from project.clj

- Drop default memory size to 512MB

- Provide means to change the Antlers delimiters from {{ to [[ (or any two chars)

- Allow indexing to be disabled

- Put template helpers in their own key in the request map and merge them into the root request on render

- Provide blacklist for various table and column names which currently break (like "order")

- Extend DB protocol to allow for NoSQL and Datomic

- Separate state from general Caribou configuration

- Heroku Support:  

    - Allow direct string config of DB

    - Enable migrations on boot

    - Heroku DB config options:  

```clj
:ssl true
:sslfactory org.postgresql.ssl.NonValidatingFactory
```

- Improve Docs:

    - Explain how to create values of all the various field types (addresses, assets, enums)

    - Explain associations

    - Improve Heroku and Immutant docs
