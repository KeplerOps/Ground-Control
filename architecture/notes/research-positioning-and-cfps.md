# Research Positioning and CFP Options

Status: exploratory note, current as of 2026-05-12.

This note situates Ground Control against adjacent engineering research
communities and identifies CFPs that are useful for orientation. This is not a
submission plan. The point is to understand which communities, formats, and
vocabularies might recognize the work if it later becomes useful to write
about it.

CFP dates move, so the venue list should be rechecked before any submission
decision.

## Research Neighborhood

### Digital Thread and Digital Engineering

Digital thread literature treats lifecycle engineering information as an
authoritative, connected flow across design, engineering, production,
operation, maintenance, and feedback. The mature version of this idea is most
visible in defense, aerospace, manufacturing, PLM, MBSE, and digital twin work.

Ground Control fits here as a software-native digital thread substrate. Its
current spine is requirements, ADRs, traceability links, baselines, audit,
quality gates, risk/control/threat-model domains, repository policy, GitHub
sync, MCP tools, and agent workflow packaging. The important distinction is
that Ground Control is not trying to make CAD/PLM digital thread tooling for
physical systems. It is pushing the same "authoritative lifecycle context"
idea into software systems and agentic development.

The research gap: digital thread work already has strong language for
authoritative sources of truth, data linkage, model integration, lifecycle
traceability, and interoperability. It has weaker treatment of autonomous
software agents acting against the thread, especially when the thread includes
requirements, code, tests, CI, incident evidence, security controls, and
healing actions.

Ground Control's strongest claim in this neighborhood:

> A software system needs an agent-operable digital thread before agents can
> safely build, maintain, operate, and heal it.

### Requirements Engineering and Traceability

Requirements engineering literature is the closest current software
engineering home. Classic traceability work distinguishes the traceability
problem before and after requirements specification. Modern traceability work
generalizes from "requirement to code" into cross-artifact relationships across
software lifecycle artifacts.

Ground Control fits this community directly:

- requirements have stable human-readable UIDs;
- requirements move through lifecycle states;
- relations form a graph;
- traceability links connect requirements to code, tests, ADRs, policy,
  documentation, proofs, GitHub artifacts, and verification results;
- status-drift analysis treats weak evidence as analysis output rather than as
  authoritative lifecycle state;
- policy gates reject workflow and traceability regressions.

The research gap: traceability research often focuses on recovering,
classifying, visualizing, or maintaining links. Ground Control's more unusual
angle is that traceability becomes a live control surface for agents: it is
used not only for after-the-fact compliance, but to constrain what work can
start, how work is reviewed, what evidence must exist before completion, and
what context agents receive.

Ground Control's strongest claim in this neighborhood:

> Traceability is not just an audit artifact; it can be the operational memory
> and policy substrate for agentic software engineering.

### Self-Adaptive Systems and Autonomic Computing

Autonomic computing and self-adaptive systems research gives the most precise
language for "maintained, operated, and healed by agents." The core control
loop is usually framed as Monitor, Analyze, Plan, Execute over Knowledge
(MAPE-K). Self-adaptive software engineering then adds requirements, models,
runtime adaptation, uncertainty, assurance, and evidence.

Ground Control currently maps most strongly to the "K" in MAPE-K: the shared
knowledge base, evidence store, constraint set, and audit surface. It is not
yet a complete autonomic manager. It does not currently close a runtime loop
from telemetry to diagnosis to patch to deploy to postmortem. It does,
however, provide the context model that such a loop would need to avoid being
just a patch generator attached to logs.

The research gap: self-adaptive systems literature has strong runtime-loop and
assurance theory, but the knowledge base is often system models, runtime state,
and assurance cases. Ground Control adds a broader software lifecycle thread:
why a system exists, how it was designed, what agents changed, which tests and
controls justified the change, and what evidence should feed back into future
requirements.

Ground Control's strongest claim in this neighborhood:

> An agentic healing loop needs lifecycle memory, not only runtime telemetry.

### Model-Based Engineering and Models at Runtime

MODELS, SAM, and related model-based engineering communities are relevant
because they study explicit system models, model transformation, model
management, digital twins, model-based monitoring, and model-based adaptation.

Ground Control is not primarily a modeling language or MBSE workbench. Its
model is the lifecycle graph and the governance semantics around that graph.
That is still close to model-based engineering: requirements, architecture
decisions, assets, risk scenarios, controls, threat models, verification
results, and policy state become first-class model elements.

The research gap: model-based work often centers on formal modeling languages,
domain-specific languages, simulations, and transformations. Ground Control's
angle is less "model generates system" and more "system activity continuously
updates a governable model that agents use for action."

Ground Control's strongest claim in this neighborhood:

> Agentic engineering needs models that are not only design-time artifacts, but
> also durable, queryable, policy-bearing runtime work context.

### Empirical Software Engineering and Design Science

If this ever becomes a publishable research artifact, the most natural method
is likely design science plus longitudinal case study:

- Design science: Ground Control is the artifact; the research contribution is
  the design problem, architecture, and validation of the artifact in context.
- Case study research: the four-repo dogfooding corpus can show how
  requirements, traceability, PRs, CI, policy failures, review findings, and
  agent workflow events evolve over time.
- Mining software repositories: GitHub PRs, issues, commits, checks, and
  policy outputs can become a dataset for agentic workflow analysis.

A future paper would be strongest if it avoided claiming "agents improve
software engineering" in general. It should ask narrower questions that Ground
Control can answer with evidence:

- Does a live requirement graph reduce traceability drift?
- Do repo-native policy gates catch workflow failures before PR review?
- What kinds of evidence do agents fail to maintain without a control plane?
- How much of an agentic development loop can be represented as structured,
  queryable lifecycle state?
- What lifecycle state is missing when trying to extend from build-time agents
  to operate-and-heal agents?

## Where Ground Control Currently Sits

Ground Control is best described as an early agentic digital thread control
plane for software systems.

It is stronger than a concept note because it has executable surfaces:

- a Spring backend with domain packages for requirements, ADRs, assets,
  baselines, controls, control packs, documents, graph, pack registry, plugins,
  projects, quality gates, risk scenarios, threat models, and verification;
- REST APIs across those domains;
- MCP tools for agents;
- repo-native policy checks;
- Flyway migrations through `V059`;
- audit and security boundaries;
- GitHub synchronization and PR-body/policy workflow surfaces;
- dogfooded architecture decisions and workflow skills.

It is not yet the full cradle-to-grave system described in the vision. The
runtime, operations, and healing part is still mostly future architecture. The
current system is the intent/design/build/evidence substrate that a later
runtime loop would need.

The clean research positioning is therefore:

> Ground Control explores whether a requirements-centered, policy-bearing,
> agent-operable digital thread can serve as the lifecycle memory for agentic
> software engineering, and later as the knowledge substrate for autonomic
> operation and healing.

That framing avoids overclaiming. It also gives a clear growth path:

1. Intent and design memory: requirements, ADRs, risks, controls, threat models.
2. Build memory: code links, tests, PRs, CI, review decisions, quality gates.
3. Release memory: artifacts, deployment config, migrations, approvals.
4. Runtime memory: topology, telemetry, incidents, SLOs, alerts, exposure.
5. Healing memory: diagnoses, patches, verification, rollout evidence.
6. Learning memory: postmortems update requirements, controls, tests, runbooks.

## CFP Shapes to Watch

The useful role of CFPs at this stage is orientation. A CFP tells us what a
community thinks counts as a contribution, what evidence it expects, and what
vocabulary it uses. That is different from saying a submission should be
attempted.

### Full Research Paper

Full research tracks are the bar for a mature research contribution. They are
useful to read now because they show what would eventually be required:
research questions, related work, method, evidence, analysis, limitations, and
artifact quality.

Ground Control might later support a research paper if there is a focused
question such as:

- Can a live requirement graph reduce traceability drift in agentic software
  development?
- What lifecycle context do agents need to move from coding tasks to safe
  maintenance and operation?
- Can repo-native policy gates make agentic development workflows more
  auditable?

That is future work. Right now, the research-track CFPs are mainly a map of
the intellectual standard, not a practical target.

### Software Engineering in Practice / Industry Paper

Practice and industry tracks ask for concrete experience: problem, context,
intervention, results, costs, failures, and lessons learned. This is probably
the most natural eventual academic shape if Ground Control remains primarily a
real engineering system rather than a controlled experiment.

The relevant future story would not be "we built a platform." It would be:

> A small engineering organization used an agent-operable requirements,
> traceability, and policy control plane to govern agentic development across
> multiple repositories; here is what changed, what broke, what was measurable,
> and what surprised us.

At the current stage, these CFPs are useful because they identify what evidence
should be preserved if the work later becomes worth writing up.

### Tool Demonstration or Data Showcase

Tool and dataset tracks ask for a runnable artifact, a clear use case, and a
demonstration that the tool enables work that is hard to do otherwise.

For Ground Control, this is a relevant shape because the system has executable
surfaces: backend, APIs, MCP tools, policy checks, GitHub integration, and
dogfooded notes/ADRs/workflows. A later demo could show an agent querying
lifecycle context, checking traceability, identifying gaps, and producing a
policy-compliant workflow artifact.

This does not mean a tool demo should be started now. It means tool-demo CFPs
are a good checklist for what a public demonstration would need: install path,
demo corpus, repeatable scenario, video, and crisp differentiation from ALM,
issue tracking, and traceability tools.

### NIER / Vision / Extended Abstract

New Ideas and Emerging Results, vision, and extended-abstract tracks are where
early framing can belong if there is a novel problem statement and a credible
prototype or motivating case.

The possible idea is:

> Agentic digital threads are a missing abstraction between agentic coding and
> autonomic software operation.

This is not "less rigorous research." It is a different format. It may be
useful if the goal becomes testing whether the framing itself resonates with
researchers before a larger empirical package exists.

### Workshop Proposal

A workshop proposal is not a way to publish Ground Control. It is a way to ask
whether a community should exist around lifecycle context, traceability, and
governance for autonomous software agents.

Possible theme:

> Agentic Digital Threads for Software Engineering and Operations

This would only make sense with co-organizers and a broader community. At this
stage, workshop CFPs are useful mainly because they show whether major
software engineering venues are making room for agentic software engineering
as a cross-cutting topic.

### Competition or Benchmark

Competition CFPs matter because they expose a gap in current agent evaluation:
most agent benchmarks test coding tasks, not lifecycle-context use.

A future Ground-Control-shaped benchmark could ask agents to:

- recover traceability links from repository history;
- classify requirement status drift;
- identify which lifecycle artifact is missing before a change can ship;
- produce PR evidence that satisfies a policy gate;
- connect incidents or review findings back to requirements and tests.

This is not a near-term submission target. It is a useful research direction to
track because it turns the vision into measurable tasks.

## Reasonable Soon

At this stage, "reasonable" means a format that can be built from the existing
system, a clear problem framing, a small amount of supporting evidence, and a
demo or talk. It does not mean a full empirical study, a benchmark, or a
completed research paper.

### Best Near-Term Options

| Rank | Venue | Format | Deadline | Why it is plausible now | What would have to exist |
| --- | --- | --- | --- | --- | --- |
| 1 | RE 2026 | Posters and Tool Demos | 2026-05-28 | Requirements Engineering is the cleanest community home for Ground Control's current state. The track asks for a 2-page extended abstract plus a 2-page annex, and explicitly accepts work in progress, posters, and innovative tool demos. | A crisp demo of requirements/traceability/policy/MCP use; screenshots or a short video; 2-3 questions for attendees about whether agent-operable traceability is a useful framing. |
| 2 | ICSME 2026 | Industry Talk Proposal | 2026-05-29 | The talk proposal does not require an accepted paper. It asks for a title, short abstract, and 3-5 minute video. This can frame Ground Control as early infrastructure for agent-assisted maintenance and evolution. | A concrete talk angle, a short demo video, and a few examples from the dogfooded repos showing lifecycle context making maintenance safer or more auditable. |
| 3 | RE 2026 | Industrial Innovation Presentation | 2026-06-15 | This is the lowest-friction near-term fit: a 1-page outline, not proceedings. It is explicitly for an innovative challenge, idea, opportunity, or call to action with supporting evidence. | A one-page call to action: requirements engineering needs agent-operable lifecycle memory; Ground Control is a working prototype; the community should help define evidence and evaluation standards. |
| 4 | SAM 2026 | Extended Abstract | Abstract 2026-06-17, paper 2026-06-24 | SAM explicitly accepts 5-page extended abstracts for preliminary or initial research where a complete evaluation has not been performed. It fits if Ground Control is framed as a lifecycle model/control surface for agents. | A 5-page position/prototype paper: model, motivating examples, related work in models-at-runtime/self-adaptive systems, and a modest future evaluation plan. |
| 5 | ICSE 2027 | NIER | 2026-10-23 | Not immediate, but very plausible without a full empirical study if the contribution is a strong new problem framing plus prototype evidence and a concrete future plan. | A 4-page paper around "agentic digital threads" as the missing abstraction between coding agents and autonomic operations; anonymized or carefully blinded prototype discussion. |
| 6 | ICSE 2027 | Tool Demonstration and Data Showcase | 2026-10-23 | Plausible if the next few months are used to make Ground Control easy to demo. It is a tool/demo track, not a full empirical study track. | Public or reviewer-accessible demo, usage instructions, 3-5 minute video, and a repeatable scenario showing an agent using lifecycle context. |

### Most Realistic First Move

The most realistic first external submission is one of:

1. **RE 2026 Posters and Tool Demos**: best if the goal is to get feedback from
   requirements/traceability people on the tool and framing.
2. **RE 2026 Industrial Innovation Presentation**: best if the goal is to test
   the problem statement with a low publication burden.
3. **ICSME 2026 Industry Talk Proposal**: best if the goal is to talk to
   maintenance/evolution people about keeping software alive with agentic
   lifecycle context.

The strongest title shape for those is not "Ground Control is research." It is
something like:

> Agent-Operable Requirements Traceability for Software Maintenance and
> Evolution

or:

> Agentic Digital Threads: Requirements as Lifecycle Memory for Software Agents

### Reasonable But More Work

SAM 2026 extended abstract is possible, but it requires writing a real 5-page
paper quickly and making the model-based/self-adaptive angle precise. It is a
good option only if the desired audience is systems modeling and
models-at-runtime.

ICSE 2027 NIER and ICSE 2027 Tool Demonstration are not "right now" options,
but they are realistic within the year without pretending a full empirical
study exists.

### Not Reasonable For This Stage

- Full research tracks at ICSE/FSE/ASE/ICSME: wrong artifact shape right now.
- ICSE 2027 Competition Track: interesting future direction, too much
  benchmark infrastructure.
- AI-ML Systems 2026 research/industry paper: possible only with a sharper
  AI-systems deployment story; less natural than RE, ICSME, SAM, or ICSE tool
  demo.
- SREcon: useful later when the runtime operation and healing story exists in
  practice.

## CFP Map

### High-Signal Venues

| Venue | Track | Current status | Why it matters now |
| --- | --- | --- | --- |
| ICSE 2027 | Research Track | Abstract 2026-06-23, paper 2026-06-30 | Defines the top-tier research-paper bar for software engineering. Useful as a standard to read against, not as a current submission target. |
| ICSE 2027 | SEIP | Submission 2026-10-23 | Shows how the community expects practice reports to be framed: real context, intervention, evidence, failures, and lessons. |
| ICSE 2027 | NIER | Submission 2026-10-23 | Relevant if the immediate output is a problem framing around agentic digital threads rather than a completed study. |
| ICSE 2027 | Tool Demonstration and Data Showcase | Submission 2026-10-23 | Useful checklist for what a public Ground Control demo or dataset would eventually need. |
| ICSE 2027 | Competition Track | Proposal 2026-06-26 | Useful for thinking about lifecycle-context benchmarks for agents, even if running a competition is much later. |
| ICSE 2027 | Workshops | Proposal 2026-06-12 | Useful for community-shaping possibilities around agentic digital threads, but only with co-organizers. |
| RE 2026 | Industrial Innovation Presentation | Outline 2026-06-15 | Requirements Engineering is a natural language home. The presentation format is a lightweight way to test vocabulary, not a claim that a full paper exists. |
| SAM 2026 | Extended Abstract | Abstract 2026-06-17, paper 2026-06-24 | Useful because SAM/MODELS communities understand models as control surfaces; Ground Control can be read as a lifecycle model for agents. |
| AI-ML Systems 2026 | Research or Industry Track | Abstract 2026-06-08, paper 2026-06-15 | Useful if the work is framed as infrastructure for reliable agentic AI systems rather than classic ALM. |

### Other Relevant Signals

| Venue | Track | Current status | Why it matters now |
| --- | --- | --- | --- |
| ASE 2026 | Tools and Datasets | Deadline 2026-05-11 AoE | The deadline has passed, but the track is a useful reference for automated software engineering artifact expectations. |
| ASE 2026 | Industry Showcase | Abstract/paper deadlines passed in April 2026 | Useful future-cycle reference for automation and developer-tooling experience reports. |
| MODELS 2026 | Research Papers | March 2026 deadlines passed | Useful future-cycle reference if Ground Control is framed as a model-based lifecycle/control graph. |
| ICSME 2026 | Industry Track | Paper 2026-05-15, talk proposal 2026-05-29 | Useful because maintenance/evolution is close to the "kept alive by agents" part of the vision; the talk proposal format is lighter than a paper. |
| ISSRE 2026 | Industry Track / Fast Abstracts / Project Highlights | Industry abstract 2026-06-28; fast abstracts 2026-06-15 | Useful once reliability, verification, runtime behavior, and healing become more central. |
| SREcon | Talks / lightning talks | 2026 Americas CFP has passed; EMEA should be watched | Practitioner signal for the operations/healing side, but Ground Control needs more runtime story before this is central. |

## What To Preserve If This Becomes Research

There is no need to pretend a research package exists now. The practical move
is to preserve evidence cheaply while the system evolves:

- dated snapshots of requirements, traceability coverage, gates, and sweeps;
- PR and CI histories around policy changes;
- examples of agent failures that policy or traceability caught;
- examples of missing lifecycle context that made an agent task harder;
- records of manual interventions and why they were needed;
- design decisions that changed because of dogfooding;
- any future runtime/incident/healing events that connect back to
  requirements, controls, tests, or ADRs.

Those records would later support an experience report, tool demo, dataset,
benchmark, or research paper if any of those paths becomes worth pursuing.

## Framing Phrases To Test

- Agentic Digital Threads for Software Engineering
- Ground Control: A Lifecycle Control Plane for Agentic Software Engineering
- From Traceability to Control: Requirements as Runtime Memory for Software
  Agents
- Beyond Agentic Coding: Lifecycle Context for Building, Operating, and Healing
  Software Systems
- A Dogfooded Case Study of Agent-Operable Requirements Traceability
- Benchmarking Agents on Lifecycle Context, Not Just Code

## References and CFPs

- DoD Digital Engineering Strategy announcement:
  <https://www.defense.gov/News/Releases/Release/Article/1567723/the-department-of-defense-announces-its-digital-engineering-strategy/>
- Bone et al., "Toward an Interoperability and Integration Framework to Enable Digital Thread":
  <https://www.mdpi.com/2079-8954/6/4/46>
- "A Literature Review of the Digital Thread":
  <https://www.mdpi.com/2079-8954/12/3/70>
- Gotel and Finkelstein, "An analysis of the requirements traceability problem":
  <https://discovery.ucl.ac.uk/749/>
- Cleland-Huang, Gotel, and Zisman, "Software and Systems Traceability":
  <https://link.springer.com/book/10.1007/978-1-4471-2239-5>
- Kephart and Chess, "The Vision of Autonomic Computing":
  <https://doi.org/10.1109/MC.2003.1160055>
- Cheng et al., "Software engineering for self-adaptive systems: A research roadmap":
  <https://doi.org/10.1007/978-3-642-02161-9_1>
- de Lemos et al., "Software Engineering for Self-Adaptive Systems: Research Challenges in the Provision of Assurances":
  <https://doi.org/10.1007/978-3-319-74183-3_1>
- Wieringa, "Design Science Methodology for Information Systems and Software Engineering":
  <https://link.springer.com/book/10.1007/978-3-662-43839-8>
- Runeson and Host, "Guidelines for conducting and reporting case study research in software engineering":
  <https://link.springer.com/article/10.1007/s10664-008-9102-8>
- "Agentic Software Engineering: Foundational Pillars and a Research Roadmap":
  <https://arxiv.org/abs/2509.06216>
- "The Rise of AI Teammates in Software Engineering (SE) 3.0":
  <https://arxiv.org/abs/2507.15003>
- "ReqToCode: Embedding Requirements Traceability as a Structural Property of the Codebase":
  <https://arxiv.org/abs/2603.13999>
- ICSE 2027 Research Track:
  <https://conf.researchr.org/track/icse-2027/icse-2027-research-track>
- ICSE 2027 SEIP:
  <https://conf.researchr.org/track/icse-2027/icse-2027-seip>
- ICSE 2027 NIER:
  <https://conf.researchr.org/track/icse-2027/icse-2027-new-ideas-and-emerging-results--nier->
- ICSE 2027 Tool Demonstration and Data Showcase:
  <https://conf.researchr.org/track/icse-2027/icse-2027-demonstrations>
- ICSE 2027 Workshops:
  <https://conf.researchr.org/track/icse-2027/icse-2027-workshops>
- ICSE 2027 Competition Track:
  <https://conf.researchr.org/track/icse-2027/icse-2027-competition-track>
- ICSE 2027 Industry Challenge:
  <https://conf.researchr.org/track/icse-2027/icse-2027-industry-challenge>
- ASE 2026 Research:
  <https://conf.researchr.org/track/ase-2026/ase-2026-research-track>
- ASE 2026 Tools and Datasets:
  <https://conf.researchr.org/track/ase-2026/ase-2026-tools-and-data-sets>
- ASE 2026 Industry Showcase:
  <https://conf.researchr.org/track/ase-2026/ase-2026-industry-showcase>
- Requirements Engineering 2026 Industrial Innovation:
  <https://conf.researchr.org/track/RE-2026/RE-2026-industrial-innovation-papers>
- MODELS 2026 Research:
  <https://conf.researchr.org/track/models-2026/models-2026-research-papers>
- SAM 2026:
  <https://sdl-forum.org/Events/SAM2026/>
- SAM 2026 submission:
  <https://sdl-forum.org/Events/SAM2026/submission.php>
- ICSME 2026 Industry Track:
  <https://conf.researchr.org/track/icsme-2026/icsme-2026-industry-track>
- ISSRE 2026 CFP:
  <https://cyprusconferences.org/issre2026/wp-content/uploads/2026/04/Call-For-Papers-ISSRE-2026-1.pdf>
- AI-ML Systems 2026 CFP:
  <https://www.aimlsystems.org/2026/wp-content/uploads/2026/02/AIMLSystems-2026-Call-for-Papers-Version-6.0.pptx-1.pdf>
- SREcon26 Americas CFP:
  <https://www.usenix.org/conference/srecon26americas/call-for-participation>
