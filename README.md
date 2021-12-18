# com.tomjkidd/undertow-pedestal

A Leiningen template for using undertow and pedestal to provide a web server

## Usage

The project's `Makefile` provide everything you'll likely want to do, but
to get started:

Run the following commands to use the template to generate a `myproject`
directory, and start the server

``` sh
make myproject-gen
make myproject-run-dev
```

Once the server is running, in a separate terminal:

``` sh
make myproject-about
make myproject-list
make myproject-add
make myproject-list
make myproject-remove
make myproject-list
```

These commands will exercise the service, which reprent a simple todo list,
and output useful results to show the basic operations are working

## https and keytool-local-trust

This project uses a submodule, `keytool-local-trust`, which is useful for
setting up local development with https.

The submodule was installed via:

``` sh
git submodule add https://github.com/tomjkidd/keytool-local-trust.git
```

This project can use the `keytool-local-trust` repo to generate a new keystore/truststore, but you have to do a little setup.

After cloning this repo, run the following to initialize and update the submodule

``` sh
git submodule init
git submodule update
```

The `backfill-keytool-local-trust` make target attempts to install the root and
server PEM certificates with OSX "Keychain Access", so that the https make targets
will work. Note that you have to manually set the trust settings for the server
certificate in order for curl to trust it without the `-k` flag!

After doing this setup, run the following commands (with the server running):

``` sh
make myproject-https-about
make myproject-https-list
make myproject-https-add
make myproject-https-list
make myproject-https-remove
make myproject-https-list
```

These should have exactly the same behavior as the http targets, except they
use https!

## License

Copyright © 2021 FIXME

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
