SOURCE=$(patsubst %.coffee,%.js,$(wildcard *.coffee))
SERVER=sapientisat.org:web/showkr/

all: $(SOURCE)

%.js: %.coffee
	coffee -pc $< > $@

deploy: $(SOURCE)
	rsync -Pr $(SOURCE) index.html $(SERVER)

watch: all
	fswatch . make
