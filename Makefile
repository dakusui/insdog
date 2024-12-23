# We use Makefile for simplifying mvn command's usage.
# Since InspektorDog (shortened "insdog") is a Java-based component, its binary build and release should be done through mvn, without relying on Makefile.

BASH:=$(shell which bash)
MVN:=source .dependencies/sdkman/bin/sdkman-init.sh && \
     sdk use java "${SDK_JDK_NAME}" &&                 \
     mvn -B -Dmaven.javadoc.skip=true
MVN_WITH_JAVADOC:=source .dependencies/sdkman/bin/sdkman-init.sh && \
	              sdk use java "${SDK_JAVADOC_JDK_NAME}" &&         \
                  mvn -B -Dmaven.javadoc.skip=false
PROJ_DIR:=$(shell pwd)

## This is a Makefile for "InspektorDog" project.
## - https://github.com/moneyforward/insdog/wiki/9-ContributionGuidelines%7CMakefile
ABOUT: help
	@echo "__ENV_RC__='${__ENV_RC__}'"
	:

## Cleans all intermediate files, which should be generated only under `target` directory.
clean: clean-mfdoc
	@$(MVN) clean

## Compiles production source code only
compile:
	@$(MVN) clean compile

## Executes unit tests
test:
	@$(MVN) clean compile test

## Generate a site of this product under `target/site` directory.
site:
	@$(MVN_WITH_JAVADOC) clean compile site
	@./src/build_tools/render-md-into-html.sh src/site/markdown target/site src/site/resources/html
	@./src/build_tools/mangle-javadoc-html-files.sh target/site/en

# Generate Javadoc under `target/site/apidocs` dir.
# Deprecated. Use `site` instead.
javadoc: site
	:

## Does "mvn package" without generating JavaDoc to save time.
package:
	@$(MVN) clean compile package

## Build.
## Internally executes `package-without-javadoc` target.
build: package
	:

## Generates wiki-site on your local.
## Generated site is found under .work/doc/wiki
## Please upgrade your local bash version by using `brew install bash`.
compile-wiki:
	$(BASH) -eu src/build_tools/mfdoc.sh compile-wiki -- "*.md:src/site/markdown:"

## Publishes generated site to the repo's wiki.
## Use with caution.
publish-wiki: clean-mfdoc compile-wiki _publish-wiki
	:

# Deploys wiki-site
_publish-wiki:
	$(BASH) -eu src/build_tools/mfdoc.sh publish-wiki

## Generates techdocs on your local.
## Generated site is found under .work/doc/techdocs
## Please upgrade your local bash version by using `brew install bash` before trying this target.
compile-techdocs:
	mvn -B pre-site
	$(BASH) -eu src/build_tools/mfdoc.sh compile-techdocs -- \
	                                                 "*.md:src/site/markdown:"

## Publishes generated site to the techdocs branch
## Use with caution.
publish-techdocs: clean-mfdoc compile-techdocs _publish-techdocs
	:

# Deploys wiki-site
_publish-techdocs:
	$(BASH) -eu src/build_tools/mfdoc.sh publish-techdocs \
	                                                 -- "*.md:src/site/markdown:"

## Cleans intermediate files generated by the `mfdoc.sh` tool.
clean-mfdoc:
	$(BASH) -eu src/build_tools/mfdoc.sh clean

## Show help.
help:
	make2help $(MAKEFILE_LIST)
