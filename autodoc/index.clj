{:namespaces
 ({:source-url nil,
   :wiki-url "caribou.action.adapter-api.html",
   :name "caribou.action.adapter",
   :doc nil}
  {:source-url nil,
   :wiki-url "caribou.api-api.html",
   :name "caribou.api",
   :doc nil}
  {:source-url nil,
   :wiki-url "caribou.app.config-api.html",
   :name "caribou.app.config",
   :doc nil}
  {:source-url nil,
   :wiki-url "caribou.app.controller-api.html",
   :name "caribou.app.controller",
   :doc nil}
  {:source-url nil,
   :wiki-url "caribou.app.template.freemarker-api.html",
   :name "caribou.app.template.freemarker",
   :doc nil}
  {:source-url nil,
   :wiki-url "caribou.app.view-api.html",
   :name "caribou.app.view",
   :doc nil}
  {:source-url nil,
   :wiki-url "caribou.core-api.html",
   :name "caribou.core",
   :doc nil}
  {:source-url nil,
   :wiki-url "caribou.db-api.html",
   :name "caribou.db",
   :doc nil}
  {:source-url nil,
   :wiki-url "caribou.debug-api.html",
   :name "caribou.debug",
   :doc nil}
  {:source-url nil,
   :wiki-url "caribou.image-api.html",
   :name "caribou.image",
   :doc nil}
  {:source-url nil,
   :wiki-url "caribou.logger-api.html",
   :name "caribou.logger",
   :doc nil}
  {:source-url nil,
   :wiki-url "caribou.migration-api.html",
   :name "caribou.migration",
   :doc nil}
  {:source-url nil,
   :wiki-url "caribou.model-api.html",
   :name "caribou.model",
   :doc nil}
  {:source-url nil,
   :wiki-url "caribou.page-api.html",
   :name "caribou.page",
   :doc nil}
  {:source-url nil,
   :wiki-url "caribou.template.adapter-api.html",
   :name "caribou.template.adapter",
   :doc nil}
  {:source-url nil,
   :wiki-url "caribou.util-api.html",
   :name "caribou.util",
   :doc nil}
  {:source-url nil,
   :wiki-url "leiningen.bootstrap-api.html",
   :name "leiningen.bootstrap",
   :doc nil}
  {:source-url nil,
   :wiki-url "leiningen.migrate-api.html",
   :name "leiningen.migrate",
   :doc nil}),
 :vars
 ({:arglists ([env]),
   :name "init",
   :namespace "caribou.app.config",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url "/caribou.app.config-api.html#caribou.app.config/init",
   :doc
   "initialize the app's config.  expects the environment (hard-coded to :production for now)",
   :var-type "function",
   :line 34,
   :file "src/caribou/app/config.clj"}
  {:arglists ([filename env]),
   :name "process-db-config",
   :namespace "caribou.app.config",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/caribou.app.config-api.html#caribou.app.config/process-db-config",
   :doc
   "given the path to a yaml config file, produce the map representing it.\nthe config is of the form:\nproduction:\n    classname: org.postgresql.Driver\n    subprotocol: postgresql\n    host: localhost\n    database: caribou\n    user: postgres",
   :var-type "function",
   :line 18,
   :file "src/caribou/app/config.clj"}
  {:arglists ([template-name]),
   :name "get-template",
   :namespace "caribou.app.template.freemarker",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/caribou.app.template.freemarker-api.html#caribou.app.template.freemarker/get-template",
   :doc "Gets a Freemarker template from the Configuration",
   :var-type "function",
   :line 16,
   :file "src/caribou/app/template/freemarker.clj"}
  {:arglists ([template-path]),
   :name "init",
   :namespace "caribou.app.template.freemarker",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/caribou.app.template.freemarker-api.html#caribou.app.template.freemarker/init",
   :doc "Set up our Freemarker config",
   :var-type "function",
   :line 8,
   :file "src/caribou/app/template/freemarker.clj"}
  {:arglists ([template root template-length] [template root]),
   :name "render",
   :namespace "caribou.app.template.freemarker",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/caribou.app.template.freemarker-api.html#caribou.app.template.freemarker/render",
   :doc "Process a Freemarker template, returns a String",
   :var-type "function",
   :line 21,
   :file "src/caribou/app/template/freemarker.clj"}
  {:arglists ([template-name]),
   :name "render-wrapper",
   :namespace "caribou.app.template.freemarker",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url
   "/caribou.app.template.freemarker-api.html#caribou.app.template.freemarker/render-wrapper",
   :doc "Wraps a template filename in a render",
   :var-type "function",
   :line 32,
   :file "src/caribou/app/template/freemarker.clj"}
  {:arglists ([locals form]),
   :name "eval-with-locals",
   :namespace "caribou.debug",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url "/caribou.debug-api.html#caribou.debug/eval-with-locals",
   :doc
   "Evals a form with given locals.  The locals should be a map of symbols to\nvalues.",
   :var-type "function",
   :line 16,
   :file "src/caribou/debug.clj"}
  {:arglists ([]),
   :name "local-bindings",
   :namespace "caribou.debug",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url "/caribou.debug-api.html#caribou.debug/local-bindings",
   :doc
   "Produces a map of the names of local bindings to their values.",
   :var-type "macro",
   :line 9,
   :file "src/caribou/debug.clj"}
  {:arglists ([]),
   :name "repl",
   :namespace "caribou.debug",
   :source-url nil,
   :raw-source-url nil,
   :wiki-url "/caribou.debug-api.html#caribou.debug/repl",
   :doc "Starts a REPL with the local bindings available.",
   :var-type "macro",
   :line 25,
   :file "src/caribou/debug.clj"})}
