.PHONY: keytool-local-trust clean-keytool-local-trust

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
