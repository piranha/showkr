all: fig

fig:
	lein figwheel dev

min:
	lein cljsbuild once min

css: resources/public/style.css

resources/public/style.css: styles/style.less styles/bootstrap/*.less
	lessc $< > $@
