# We use Makefile for simplifying mvn command's usage.
# Since autotest-ca is a Java-based component, its binary build and release should be done through mvn, without relying on Makefile.

BASH:=$(shell which bash)
PROJ_DIR:=$(shell pwd)

## This is a Makefile for "autotest-ca" project.
## - https://github.com/moneyforward/autotest-ca/wiki/9-ContributionGuidelines%7CMakefile
ABOUT: help
	:

## Cleans all intermediate files, which should be generated only under `target` directory.
clean: clean-mfdoc
	mvn clean

## Compiles production source code only
compile:
	mvn clean compile
## Executes unit tests
test:
	mvn clean compile test
## Generates wiki-site on your local.
## Generated site is found under .work/doc/wiki
## Please upgrade your local bash version by using `brew install bash`.
compile-wiki:
	$(BASH) -eu $(PROJ_DIR)/src/build_tools/mfdoc.sh compile-wiki -- "*.md:src/site/markdown:"

## Publishes generated site to the repo's wiki.
## Use with caution.
publish-wiki: clean-mfdoc compile-wiki _publish-wiki
	:

# Deploys wiki-site
_publish-wiki:
	$(BASH) -eu $(PROJ_DIR)/src/build_tools/mfdoc.sh publish-wiki

## Generates wiki-site on your local.
## Generated site is found under .work/doc/wiki
## Please upgrade your local bash version by using `brew install bash`.
compile-techdocs:
	$(BASH) -eu $(PROJ_DIR)/src/build_tools/mfdoc.sh compile-techdocs \
	                                                 -- "*.md:src/site/markdown:"

## Publishes generated site to the techdocs branch
## Use with caution.
publish-techdocs: clean-mfdoc compile-techdocs _publish-techdocs
	:

# Deploys wiki-site
_publish-techdocs:
	$(BASH) -eu $(PROJ_DIR)/src/build_tools/mfdoc.sh publish-techdocs \
	                                                 -- "*.md:src/site/markdown:"

## Cleans intermediate files generated by the `mfdoc.sh` tool.
clean-mfdoc:
	$(BASH) -eu $(PROJ_DIR)/src/build_tools/mfdoc.sh clean

## Generate Javadoc under `target/site/apidocs` dir.
javadoc:
	mvn clean compile test javadoc:javadoc

## Run
run-all-tests:
	java -jar target/autotest-caweb.jar -q 'classname:~.*' run

## Creates a autotest-caweb.jar without javadoc to save time
package_without_javadoc: target/autotest-caweb.jar
	mvn -Dmaven.javadoc.skip=true clean compile package

help:
	make2help $(MAKEFILE_LIST)