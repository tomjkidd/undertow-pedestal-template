.PHONY: keytool-local-trust clean-keytool-local-trust backfill-keytool-local-trust

# Generate a keystore/truststore
# These commands should only seldom be needed. They are useful for
# generating a new working keystore/truststore when the committed ones
# expire. See that README and Makefile for more information.
#
# Note that they call the git submodule's Makefile, recursively:
# https://www.gnu.org/software/make/manual/html_node/Recursion.html
keytool-local-trust:
	cd keytool-local-trust && $(MAKE)

clean-keytool-local-trust:
	cd keytool-local-trust && $(MAKE) clean

# TODO: It'd be nice if the keytool-local-trust Makefile could handle this.
# The idea is to dig the root and server PEM files out of the server keystore, and then load those into OSX
# so that the server can use https without having to start by generating new keytool-local-trust bits.
# Those steps should really only be necessary when the project certificates expire.
# With the way that the targets are currently specified, just wanted to have the defaults hard-coded here
# to get unblocked using the keystore/truststore of this project.
# Also note: You have to go into the OSX "Keychain Access" program and set it to trust the localhost certificate!
backfill-keytool-local-trust:
	cp resources/leiningen/new/undertow_pedestal/undertow-pedestal-keystore.jks keytool-local-trust/keystore.jks
	cp resources/leiningen/new/undertow_pedestal/undertow-pedestal-truststore.jks keytool-local-trust/truststore.jks
	cd keytool-local-trust && keytool -keystore keystore.jks -storepass password -exportcert -rfc -alias root > root.pem
	cd keytool-local-trust && keytool -keystore keystore.jks -storepass password -exportcert -rfc -alias server > server.pem
	cd keytool-local-trust && $(MAKE) osx-add-root-pem-to-keychain
	cd keytool-local-trust && $(MAKE) osx-add-server-pem-to-keychain

# ============================
# Testing the template locally
# ============================

myproject-gen:
	lein new com.tomjkidd/undertow-pedestal myproject --force

# ======================================
# Running the generated template locally
# ======================================

myproject-repl:
	cd myproject && lein repl

myproject-run-dev:
	cd myproject && lein run-dev

# ===============
# http operations
# ===============

myproject-about:
	curl http://localhost:8080/about -i -k

myproject-list:
	curl http://localhost:8080/ping -H "Content-Type: application/json" -X POST -d '{"result":{"action":"item.list","parameters":{}}}'

myproject-add:
	curl http://localhost:8080/ping -H "Content-Type: application/json" -X POST -d '{"result":{"action":"item.add","parameters":{"item":"apple"}}}'

myproject-remove:
	curl http://localhost:8080/ping -H "Content-Type: application/json" -X POST -d '{"result":{"action":"item.remove","parameters":{"item":"apple"}}}'

# ================
# https operations
# ================

myproject-https-about:
	curl https://localhost:8443/about -i -k

myproject-https-list:
	curl https://localhost:8443/ping -H "Content-Type: application/json" -X POST -d '{"result":{"action":"item.list","parameters":{}}}' -i -k

myproject-https-add:
	curl https://localhost:8443/ping -H "Content-Type: application/json" -X POST -d '{"result":{"action":"item.add","parameters":{"item":"apple"}}}' -i -k

myproject-https-remove:
	curl https://localhost:8443/ping -H "Content-Type: application/json" -X POST -d '{"result":{"action":"item.remove","parameters":{"item":"apple"}}}' -i -k
