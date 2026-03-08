.PHONY: setup server export validate clean

VENV := .venv
PYTHON := $(VENV)/bin/python
STRICTDOC := $(VENV)/bin/strictdoc
DOCS_DIR := ../../docs/requirements

setup: $(VENV)/bin/strictdoc ## Create venv and install strictdoc

$(VENV)/bin/strictdoc:
	python3 -m venv $(VENV)
	$(VENV)/bin/pip install strictdoc

server: setup ## Launch StrictDoc web UI for editing requirements
	$(STRICTDOC) server $(DOCS_DIR)

export: setup ## Export requirements to HTML
	$(STRICTDOC) export $(DOCS_DIR) --output-dir output/

validate: setup ## Validate requirements traceability
	$(STRICTDOC) export $(DOCS_DIR) --output-dir /dev/null 2>&1 || true

clean: ## Remove venv and output
	rm -rf $(VENV) output/

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-15s\033[0m %s\n", $$1, $$2}'

.DEFAULT_GOAL := help
