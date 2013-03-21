# Quickstart Guide

Once you have _lein_ installed and working, you can create
your first Caribou application:

    lein caribou create <application>

This will make the project structure, and create a database
(using the pure Java database, (h2)[http://www.h2database.com/])
ready for you to start developing with.

You can start your new application and begin creating content
immediately:

    cd <application>
    lein caribou start

Navigate to [http://localhost:33333] to see a skeleton
homepage, and follow the link to login to the administrative
back-end tool.  A user _caribou_ has been created for you,
with password _caribou_.
