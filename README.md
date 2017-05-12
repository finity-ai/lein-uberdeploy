# lein-uberdeploy

A Leiningen plugin which is for `uberjar` what `deploy` plugin is for `jar`.


## Usage

Use this for user-level plugins:

Put `[lein-uberdeploy "0.1.0-SNAPSHOT"]` into the `:plugins` vector of your `:user`
profile.

Use this for project-level plugins:

Put `[lein-uberdeploy "0.1.0-SNAPSHOT"]` into the `:plugins` vector of your project.clj.

Deploy your uberjar:

    $ lein uberdeploy

## License

Copyright © 2017 Finity.ai

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
