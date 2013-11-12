# Debugging Caribou

Currently for debugging we use the spectacular
[Schmetterling](https://github.com/prismofeverything/schmetterling) library for
attaching to a running Caribou process and freezing execution when exceptions
occur.

When Caribou boots it displays near the top of its output a `dt_socket` port.
Start up Schmetterling in another tab and enter this port.  Then, when an
exception occurs in your Caribou app, an interactive stacktrace will appear in
the Schmetterling tab.  You can evaluate expressions in your code in the context
of where the exception was thrown, giving you access to the state at the instant
something went wrong.

Schmetterling is currently under heavy development and will soon support
features such as source viewing, setting breakpoints and stepping through
execution flow.
