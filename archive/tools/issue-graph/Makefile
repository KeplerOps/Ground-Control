.PHONY: setup run validate critical-path stale blocking sdoc-gaps export clean

VENV := .venv
PYTHON := $(VENV)/bin/python

setup: $(VENV)/bin/python ## Create venv and install dependencies

$(VENV)/bin/python:
	python3 -m venv $(VENV)
	$(VENV)/bin/pip install -e .

run: setup ## Fetch issues and run all analyses
	$(PYTHON) issue_graph.py --all

validate: setup ## Validate graph for problems
	$(PYTHON) issue_graph.py --cache --validate

critical-path: setup ## Show critical path
	$(PYTHON) issue_graph.py --cache --critical-path

stale: setup ## Find stale/obsolete issues
	$(PYTHON) issue_graph.py --cache --stale

blocking: setup ## Show top blocking issues
	$(PYTHON) issue_graph.py --cache --blocking

sdoc-gaps: setup ## Check sdoc ↔ GitHub issue traceability
	$(PYTHON) issue_graph.py --cache --sdoc-gaps

export: setup ## Export graph as JSON
	$(PYTHON) issue_graph.py --cache --export graph.json

clean: ## Remove venv and cache
	rm -rf $(VENV) .issue_cache.json graph.json

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-15s\033[0m %s\n", $$1, $$2}'

.DEFAULT_GOAL := help
