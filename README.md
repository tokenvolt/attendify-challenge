# attendify-challenge

## url-matcher

```ShellSession
$ lein deps
$ lein repl
```

```clojure
> (require '[attendify-challenge.url-matcher.core :as url-matcher])
nil

> (def pattern (url-matcher/new-pattern "host(dribbble.com); path(?user/shots/?id); queryparam(offset=?offset); queryparam(list=?type);"))
#'attendify-challenge.dribbble.core/pattern

> (url-matcher/recognize pattern "https://dribbble.com/bob/shots/1905065-Travel-Icons-pack?list=users&offset=1")
[[:user "bob"] [:id "1905065-Travel-Icons-pack"] [:type "users"] [:offset "1"]]
```

## dribbble

```ShellSession
$ lein deps
$ lein run <id or username of Dribbble user>
```
