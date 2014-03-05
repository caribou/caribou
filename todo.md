# TODO

*- Integrate Polaris into frontend routing

*- Add .gitkeep to /app

*- Remove immutant dependency from project.clj

*- Drop default memory size to 512MB

*- Fix issue with triggering routes of different HTTP methods from the same route

*- Fix error in API

- Provide means to change the Antlers delimiters from {{ to [[ (or any two chars)

*- Allow indexing to be disabled

*- Put template helpers in their own key in the request map and merge them into the root request on render

- Provide blacklist for various table and column names which currently break (like "order")

- Separate state from general Caribou configuration

- Extend DB protocol to allow for NoSQL and Datomic

* - Heroku Support

- Improve Docs:

    - Add section for clojurescript

    *- Boot http-kit rather than ring now (lein run vs lein ring server)

    - Explain how to create values of all the various field types (addresses, assets, enums)

    - Explain associations

    *- Show how to swap out new template renderer

    *- Improve Heroku and Immutant docs

*- Make upgrade doc for existing caribou sites

*- Update Roadmap
