# Deploying Caribou

## Ring Server (Jetty)

Using the built-in ring server is the simplest approach.  Simply run:

```bash
    % lein ring server
```

at the project root and your site will come to life!  To set the port you can
change the options that live in your `project.clj`:

```clj
  :ring {:handler taiga.core/handler
         :init taiga.core/init
         :port 33333})
```

Set the port to any viable port number and restart!

## Tomcat

For Tomcat, the process is simple.  First, change your `{:assets {:dir ...}}`
from `"app/"` to something absolute on your filesystem (as you cannot add files
into your deployed uberwar).

Then build the uberwar:

```bash
    % lein ring uberwar
```

Once this has completed, drop the resulting jar into your running Tomcat
container's webapps directory.  Voila!

## Immutant (JBoss)

To deploy to Immutant, set up the
[lein-immutant](http://github.com/immutant/lein-immutant) plugin and then in
your Caribou project root simply type:

```bash
    % lein immutant deploy
```

Immutant is configured through the `:immutant` key in your project.clj.  See the
[Immutant docs](http://immutant.org/) for help.

## Beanstalk

To deploy to Beanstalk running Tomcat, the key is to use the
[lein-beanstalk](http://github.com/weavejester/lein-beanstalk) plugin and have
the right set of values in your `project.clj`.  Here is an example configuration:

```clj
:aws {:access-key "YOUR-AWS-ACCESS-KEY"
      :secret-key "YOUR-AWS-SECRET-KEY"
      :region "us-west-1"
      :beanstalk {:region "us-west-1"
                  :s3-region "us-west-1"
                  :app-name "taiga"
                  :s3-bucket "taiga-prod"
                  :environments [{:name "taiga-production"
                                  :env {"environment" "production"}}]}}
```

Then, run the `lein-beanstalk` command:

```bash
    % lein beanstalk deploy
```

If your Beanstalk configuration with AWS is set up right, you now have a Caribou
project running in the cloud somewhere!  Congratulations.

## Heroku

Caribou by default is already set up to deploy to Heroku.  The main thing to
deal with is setting up and migrating the Postgresql database.

We will start from the beginning.  

* Provision a new Caribou app:

```bash
    % lein new caribou orbmaster
    % cd orbmaster
```

* Create the heroku app and provision a new Postgresql database:

```bash
    % git init
    % heroku apps:create
    % heroku addons:add heroku-postgresql:dev
    % heroku config:set CARIBOU_ENVIRONMENT=heroku
```

* Find the heroku credentials for your remote db and populate your
  `resources/config/heroku.clj` config file with the right values (this is the
  most elaborate part).  The values to swap out will be obvious by their
  `heroku-` prefix in the map under the `:database` key.  This is only necessary
  for migration, once deployed your app will just use the heroku supplied
  environment variable `DATABASE_URL` for its database information:

```bash
    % heroku config | grep HEROKU_POSTGRESQL             # note the color!
    % heroku pg:promote HEROKU_POSTGRESQL_{{color}}_URL  # replace with your color!
    % heroku pg:credentials DATABASE   # note host, port, database, user and password
    % vim resources/config/heroku.clj  # add values discovered from previous command!
```

* Build the base Caribou tables in the remote database using `lein caribou
  migrate` (This takes a long time, let it run to completion!):
  
```bash
    % lein caribou migrate resources/config/heroku.clj
```

* Commit the git repository and and push it to heroku:

```bash
    % git add .
    % git commit -m "init"
    % git push heroku master
```

* Finally, start the dyno!

```bash
    % heroku ps:scale web=1
```

* Once complete, open the deployed site:

```bash
    % heroku open
```

You should see your new app up and running!  

If you want to use the Caribou image support, you have to set up an s3 bucket
(as heroku does not have a persistent filesystem).  To do this, add the
following entry to your `resources/config/heroku.clj` config file:

```clj
  :aws {:bucket "your.bucket.name"
        :credentials {:access-key "YOUR-ACCESS-KEY"
                      :secret-key "YOUR-SECRET-KEY"}}
```

This will allow uploading images in the admin and resizing images from templates
(or elsewhere).

Most of the above is standard Heroku procedure.  For any additional Heroku
support, refer to the
[Heroku docs](http://devcenter.heroku.com/articles/clojure).


