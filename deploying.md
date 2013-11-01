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

For Tomcat, the process is simple.

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

There is a generated Immutant configuration file that lives in
`src/immutant/init.clj`.  Any additional Immutant configuration can be done
there.  See the [Immutant docs](http://immutant.org/) for help.

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

Caribou by default is already set up to deploy to Heroku.  The main key is to
create a git repo and set the remote heroku target:

* set up the git repository

```bash
    % git init
    % git add .
    % git commit -m "init"
```

* create the heroku remote and deploy

```bash
    % heroku apps:create
    % git push heroku master
    % heroku ps:scale web=1
```

* open the deployed site

```bash
    % heroku open
```

For any additional Heroku support, refer to the [Heroku docs](http://devcenter.heroku.com/articles/clojure).


