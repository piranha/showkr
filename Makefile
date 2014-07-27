all: fig

fig:
	lein figwheel dev

min:
	lein cljsbuild once min
