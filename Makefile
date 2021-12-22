.PHONY: keytool-local-trust clean-keytool-local-trust backfill-keytool-local-trust

# https://www.linuxcommand.org/lc3_adv_tput.php
# tput setaf <fg_color> - used to set foreground color
# tput sgr0 - used to reset
Blue := $(shell tput setaf 69)
Red := $(shell tput setaf 1)
Green := $(shell tput setaf 2)
Yellow := $(shell tput setaf 3)
Orange := $(shell tput setaf 214)
None := $(shell tput sgr0)

color-check: tput-check

tput-check:
	@echo "$(Blue)This should be blue$(None)"
	@echo "$(Green)This should be green$(None)"
	@echo "$(Yellow)This should be yellow$(None)"
	@echo "$(Orange)This should be orange$(None)"
	@echo "$(Red)This should be red$(None)"
	@echo "This should be the default"

# Generate a keystore/truststore
# These commands should only seldom be needed. They are useful for
# generating a new working keystore/truststore when the committed ones
# expire. See that README and Makefile for more information.
#
# Note that they call the git submodule's Makefile, recursively:
# https://www.gnu.org/software/make/manual/html_node/Recursion.html
keytool-local-trust:
	@echo "$(Blue)Running keytool-local-trust$(None)"
	cd keytool-local-trust && $(MAKE)

clean-keytool-local-trust:
	cd keytool-local-trust && $(MAKE) clean

replace-ssl-files:
	cp keytool-local-trust/keystore.jks resources/leiningen/new/undertow_pedestal/ssl/keystore.jks
	cp keytool-local-trust/truststore.jks resources/leiningen/new/undertow_pedestal/ssl/truststore.jks
	cp keytool-local-trust/server.csr resources/leiningen/new/undertow_pedestal/ssl/server.csr
	cp keytool-local-trust/server.crt resources/leiningen/new/undertow_pedestal/ssl/server.crt
	cp keytool-local-trust/server.pem resources/leiningen/new/undertow_pedestal/ssl/server.pem
	cp keytool-local-trust/root.pem resources/leiningen/new/undertow_pedestal/ssl/root.pem

# TODO: It'd be nice if the keytool-local-trust Makefile could handle this.
# The idea is to dig the root and server PEM files out of the server keystore, and then load those into OSX
# so that the server can use https without having to start by generating new keytool-local-trust bits.
# Those steps should really only be necessary when the project certificates expire.
# With the way that the targets are currently specified, just wanted to have the defaults hard-coded here
# to get unblocked using the keystore/truststore of this project.
# Also note: You have to go into the OSX "Keychain Access" program and set it to trust the localhost certificate!
backfill-keytool-local-trust:
	cp resources/leiningen/new/undertow_pedestal/ssl/root.pem keytool-local-trust/root.pem
	cp resources/leiningen/new/undertow_pedestal/ssl/server.pem keytool-local-trust/server.pem

osx-install-pem-files: backfill-keytool-local-trust
	cd keytool-local-trust && $(MAKE) osx-add-root-pem-to-keychain
	cd keytool-local-trust && $(MAKE) osx-add-server-pem-to-keychain

rotate-ssl-files:
	@echo "$(Blue)Rotating the ssl files$(None)"
	$(MAKE) keytool-local-trust
	$(MAKE) replace-ssl-files
	$(MAKE) osx-install-pem-files

# ============================
# Testing the template locally
# ============================

myproject-gen:
	@echo "$(Blue)Generating myproject locally using the undertow-pedestal template$(None)"
	lein new com.tomjkidd/undertow-pedestal myproject --force

# ======================================
# Running the generated template locally
# ======================================

myproject-repl:
	@echo "$(Blue)Running the dev repl$(None)"
	cd myproject && lein repl

myproject-run-dev:
	@echo "$(Blue)Running the dev server$(None)"
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
