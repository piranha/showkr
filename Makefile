SRC = $(shell find src)
SRC_STATIC = $(shell find resources/public -name '*.gif' -or -name '*.png' -or -name '*.ico')

.PHONY: www

dev:
	boot dev

prod: $(SRC) $(SRC_STATIC)
	@touch prod
	boot prod

deploy:
	rsync -Pr prod/ maia:showkr/
