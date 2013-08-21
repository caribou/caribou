# Philosophy

If there could be said to be one overriding design philosophy in Caribou it is
that everything in Caribou is data.  Pages are data.  Locales are data.  The
data schema itself is data that can be manipulated the same as any other data.
The arguments to all of the core Caribou functions are data, and don't use any
macros or keyword arguments or syntactic sugar of any kind.  

This is intentional, because if everything is data everything can be controlled
programmatically.  In a way, this philosophy is an extension of the basic Lisp
philosophy that programs themselves are data that can be constructed using the
same data primitives that programs use to manipulate any other data structures.
This is why Caribou uses Clojure in the first place, because Clojure is a Lisp
and Lisp was created around the concept of code as data.  The entire design of
Lisp originally was to create the simplest language where the code could be
manipulated by the code itself.  Everything else in Lisp's design flows from a
dedication to this simple idea.  There have been countless incarnations each
with their own angle, their own approach and philosophy, but the core has always
remained true to this notion of code as data, and it is why Lisp is not any one
of its implementations, but something beyond all of them.

We believe this timeless notion is not just beautiful, but practical, and we are
grateful that the existence of Clojure allows us to follow through with the
original possibilities of code as data to its full implications in terms of
EVERYTHING as data.  Our goal is to allow this potential, that any aspect of the
system is able to be harnessed programmatically, reach its ultimate
realization.

