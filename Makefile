.PHONY: deploy watch

TEMPLATES=$(wildcard app/templates/*.eco)
_SOURCE=util api models viewing browsing showkr
SOURCE=$(_SOURCE:%=build/%.js)
CSS=build/style.css
DEPS=build/ender.js
VENDOR=$(wildcard vendor/*.js)

SERVER=sapientisat.org:web/showkr/

#
# Macros
#

# activate virtualenv if exists
namespacer = [ -d ~/.virtualenvs/default ] && \
	. ~/.virtualenvs/default/bin/activate && \
	./css-namespacer.py $(1) > $(2) || \
	./css-namespacer.py $(1) > $(2)


#
# Building
#

all: $(TEMPLATES:app/%=build/%.js) $(SOURCE) $(CSS) $(VENDOR:%=build/%) build/index.html build/favicon.ico

build/%.css: %.less
	@mkdir -p $(@D)
	lessc $< $@

build/%.js: app/%.coffee $(DEPS)
	@mkdir -p $(@D)
	coffee -pc $< > $@

build/templates/%.js: app/templates/% node_modules/eco
	@mkdir -p $(@D)
	./eco.js $< $(<:app/%=%) > $@

build/vendor/%.js: vendor/%.js
	@mkdir -p $(@D)
	cp $< $@

build/index.html: index.html $(SOURCE) $(VENDOR) $(DEPS)
	@mkdir -p $(@D)
	DEPS="$(DEPS:build/%=%) $(TEMPLATES:app/%=%.js) $(VENDOR) $(SOURCE:build/%=%)" awk -f build.awk $< > $@

%/static: static
	rsync -r $</ $@/ 

node_modules/eco:
	npm install eco

$(DEPS):
	@mkdir -p $(@D)
	ender build -o $@ qwery bean reqwest backbone keymaster
	sed -i '' 's:root.Zepto;$\:root.ender;:' $@

%/favicon.ico: favicon.ico
	@mkdir -p $(@D)
	cp $< $@

#
# Deployment
#

prod: all prod/app.js prod/index.html prod/style.css prod/namespaced.css prod/embed.js prod/favicon.ico

deploy: prod
	rsync -Pr prod/ $(SERVER)

prod/embed.js: embed.coffee
	@mkdir -p $(@D)
	coffee -cpb $< > $@

prod/style.css: build/style.css
	@mkdir -p $(@D)
	$(call namespacer, compress $<, $@)

prod/namespaced.css: build/style.css
	@mkdir -p $(@D)
	$(call namespacer, namespace $<, $@)

prod/app.js: $(DEPS) $(VENDOR) $(TEMPLATES:app/%=build/%.js) $(SOURCE)
	@mkdir -p $(@D)
	cat $^ | uglifyjs > $@

prod/index.html: index.html prod/app.js
	@mkdir -p $(@D)
	DEPS="app.js?$(shell md5 -q prod/app.js)" awk -f build.awk $< > $@


# Utility

clean:
	rm -rf build prod

watch: all
	fswatch . make

watch-prod: prod
	fswatch . 'make prod'

info: $(DEPS)
	ender info --use $<

open:
	open http://localhost/showkr
