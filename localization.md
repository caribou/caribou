## Content Localization

Localizing content in Caribou means providing different values for the fields in
a content item depending on what "locale" the application is receiving requests
from.  Localization of a Model is done on a field by field basis.  This means
that even what items are associated to what can be localized if desired.

To begin, let's create a model that will hold content that varies between
locales (consider this example to be entirely contrived):

```clj
(caribou.model/create 
  :model 
  {:name "Wisdom" 
   :fields [{:name "quotation" 
             :type "string" 
             :localized true}]})
```

Notice the line `:localized true`.  This signifies that values stored in this
field will have different values based on which locale is being requested.

Next, let's create a new locale.  Because this is a tutorial, we will create a
locale for Klingon (complete with utterly fabricated locale code):

```clj
(caribou.model/create 
  :locale 
  {:language "Klingon" 
   :region "Qo'noS" 
   :code "ql-QN"})
```

These are the three required fields for created a locale.  Notice that creating
a locale is exactly the same as creating any other content in Caribou.  Locale
is a model.  Everything is a model.  Even Model is a model.

Next, let's create a new instance of our new Wisdom model.  This is easy, we
know how to do this:

```clj
(caribou.model/create
  :wisdom
  {:quotation "Trust, but verify"})
```

To get the basic instance back, we can call gather on the Wisdom model:

```clj
(caribou.model/gather :wisdom)

---> ({:id 1 :quotation "Trust, but verify" ...})
```

But the whole point is to pull the content for our new Klingon locale, "ql-QN".
To do this, we simply specify the locale code in the gather:

```clj
(caribou.model/gather :wisdom {:locale "ql-QN"})

---> ({:id 1 :quotation "Trust, but verify" ...})
```

This is great, but it still has the same value.  This is because we haven't
specified what the localized value should be.  To do that, let's call
`caribou.model/update` with the right locale:

```clj
(caribou.model/update 
  :wisdom
  1
  {:quotation "yIvoq 'ach yI'ol"}
  {:locale "ql-QN"})
```

Notice how `update` takes a second map.  The first map is only for specifying
what values the content has, while the second is full of modifiers and options
that won't actually be directly committed as values for this instance.

Now when we do our gather, we get the right values:

```clj
(caribou.model/gather :wisdom {:locale "ql-QN"})

---> ({:id 1 :quotation "yIvoq 'ach yI'ol" ...})
```

Whereas the original non-localized version still exists:

```clj
(caribou.model/gather :wisdom)

---> ({:id 1 :quotation "Trust, but verify" ...})
```

This non-localized version is actually part of the "global" locale, which is
always present.  The "global" locale also supplies values for instances that don't
have a value in the localized field.  So until a specific value is given to the
`quotation` for the "ql-QN" locale, it will inherit the value that exists in
"global".  This allows you to just override the content that needs to be
overridden and provide, for instance, the same image in all locales except the
specific ones that need their own image.

