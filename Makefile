.PHONY: deploy watch

SOURCE=$(patsubst app/%.coffee,build/%.js,$(wildcard app/*.coffee))
CSS=build/style.css
DEPS=build/ender.js

SERVER=sapientisat.org:web/showkr/

all: $(SOURCE) $(CSS) build/index.html

build/%.css: %.less
	@mkdir -p $(@D)
	lessc $< $@

build/%.js: app/%.coffee $(DEPS)
	@mkdir -p $(@D)
	coffee -pc $< > $@

build/index.html: index.html
	@mkdir -p $(@D)
	DEPS="$(DEPS:build/%=%) $(SOURCE:build/%=%)" awk -f build.awk $< > $@

%/static: static
	rsync -r $</ $@/ 

$(DEPS):
	@mkdir -p $(@D)
	ender build -o $@ qwery bean bonzo reqwest backbone keymaster


# Deployment

prod: all prod/app.js prod/index.html
	@touch prod
	cp $(CSS) prod/

deploy: prod
	rsync -Pr prod/ $(SERVER)

prod/app.js: $(SOURCE)
	@mkdir -p $(@D)
	ender compile --level simple --use $(DEPS) $(SOURCE)
	mv build/ender-app.js $@

prod/index.html: index.html prod/app.js
	@mkdir -p $(@D)
	DEPS="app.js?$(shell md5 -q prod/app.js)" awk -f build.awk $< > $@


# Utility

watch: all
	fswatch . make

info: $(DEPS)
	ender info --use $<

open:
	open http://localhost/showkr
