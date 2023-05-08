.PHONY: serve
serve:
	docker compose up --build

.PHONY: htmltest
htmltest:
	docker build -t seed-docs -f Dockerfile .
	docker build -t htmltest -f Dockerfile-htmltest .
	docker run --rm htmltest
