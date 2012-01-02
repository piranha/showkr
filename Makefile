.PHONY: deploy watch

_SOURCE=util api models viewing browsing showkr
SOURCE=$(_SOURCE:%=build/%.js)
CSS=build/style.css
DEPS=build/ender.js
VENDOR=$(wildcard vendor/*.js)

SERVER=sapientisat.org:web/showkr/

all: $(SOURCE) $(CSS) $(VENDOR:%=build/%) build/index.html

build/%.css: %.less
	@mkdir -p $(@D)
	lessc $< $@

build/%.js: app/%.coffee $(DEPS)
	@mkdir -p $(@D)
	coffee -pc $< > $@

build/vendor/%.js: vendor/%.js
	@mkdir -p $(@D)
	cp $< $@

build/index.html: index.html $(SOURCE) $(VENDOR) $(DEPS)
	@mkdir -p $(@D)
	DEPS="$(DEPS:build/%=%) $(VENDOR) $(SOURCE:build/%=%)" awk -f build.awk $< > $@

%/static: static
	rsync -r $</ $@/ 

$(DEPS):
	@mkdir -p $(@D)
	ender build -o $@ jeesh reqwest backbone keymaster ender-overlay
	sed -i '' 's:root.Zepto;$\:root.ender;:' $@


# Deployment

prod: all prod/app.js prod/index.html
	@touch prod
	cp $(CSS) prod/

deploy: prod
	rsync -Pr prod/ $(SERVER)

prod/app.js: $(SOURCE)
	@mkdir -p $(@D)
	ender compile --level simple --use $(DEPS) $(VENDOR) $(SOURCE)
	mv build/ender-app.js $@

prod/index.html: index.html prod/app.js
	@mkdir -p $(@D)
	DEPS="app.js?$(shell md5 -q prod/app.js)" awk -f build.awk $< > $@


# Utility

clean:
	rm -rf build prod

watch: all
	fswatch . make

info: $(DEPS)
	ender info --use $<

open:
	open http://localhost/showkr
