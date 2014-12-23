SRC_STATIC = $(shell find resources/public -name '*.gif' -or -name '*.png' -or -name '*.ico')
STATIC = $(SRC_STATIC:resources/public/%=www/%)

.PHONY: www

all: css fig

fig:
	lein trampoline figwheel dev

min:
	lein trampoline cljsbuild auto min

www: www/showkr.min.js www/style.css www/namespaced.css $(STATIC)

## building

css: resources/public/style.css

resources/public/style.css: styles/style.less styles/bootstrap/*.less
	@mkdir -p $(@D)
	lessc $< > $@

## production building

www/%: resources/public/%
	@mkdir -p $(@D)
	cp $< $@

www/showkr.min.js: $(shell find src -name '*.cljs')
	lein trampoline cljsbuild once min

www/namespaced.css: www/style.css
	./css-namespacer.py namespace $< > $@

