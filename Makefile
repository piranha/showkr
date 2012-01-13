.PHONY: deploy watch

TEMPLATES=$(patsubst app/%,%.js,$(wildcard app/templates/*.eco))
SOURCE=$(patsubst %,%.js,util api models viewing browsing showkr)
STATIC=$(patsubst static/%,%,$(wildcard static/*))
CSS=style.css
DEPS=ender.js
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

all: $(addprefix build/,\
	$(TEMPLATES) $(SOURCE) $(CSS) $(VENDOR) $(STATIC) index.html)

build/%.css: %.less
	@mkdir -p $(@D)
	lessc $< $@

build/%.js: app/%.coffee $(addprefix build/,$(DEPS))
	@mkdir -p $(@D)
	coffee -pc $< > $@

build/templates/%.js: app/templates/% node_modules/eco
	@mkdir -p $(@D)
	./eco.js $< $(<:app/%=%) > $@

build/vendor/%.js: vendor/%.js
	@mkdir -p $(@D)
	cp $< $@

build/index.html: index.html $(addprefix build/,\
		$(SOURCE) $(VENDOR) $(DEPS) $(TEMPLATES))
	@mkdir -p $(@D)
	DEPS="$(DEPS) $(TEMPLATES) $(VENDOR) $(SOURCE)" awk -f build.awk $< > $@

build/$(DEPS):
	@mkdir -p $(@D)
	ender build -o $@ qwery bean reqwest backbone keymaster
	sed -i '' 's:root.Zepto;$\:root.ender;:' $@

build/%: static/%
	@mkdir -p $(@D)
	cp $< $@

node_modules/eco:
	npm install eco

#
# Deployment
#

prod: all $(addprefix prod/,\
	$(STATIC) app.js index.html style.css namespaced.css embed.js)

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

prod/app.js: $(addprefix build/,\
		$(DEPS) $(VENDOR) $(TEMPLATES) $(SOURCE))
	@mkdir -p $(@D)
	cat $^ | uglifyjs > $@

prod/index.html: index.html prod/app.js
	@mkdir -p $(@D)
	DEPS="app.js?$(shell md5 -q prod/app.js)" awk -f build.awk $< > $@

prod/%: static/%
	@mkdir -p $(@D)
	cp $< $@


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
