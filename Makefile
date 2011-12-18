SOURCE=$(patsubst %.coffee,%.js,$(wildcard *.coffee))

all: $(SOURCE)

%.js: %.coffee
	coffee -pc $< > $@

watch: all
	fswatch . make
