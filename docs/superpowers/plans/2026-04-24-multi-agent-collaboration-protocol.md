# Multi-Agent Collaboration Protocol

This document defines how multiple agents collaborate on this project without conflicting edits, broken contracts, or hidden scope creep.

## Goals

- maximize parallelism,
- minimize merge conflicts,
- preserve deterministic workflow boundaries,
- keep the tutor agent scoped to article learning,
- make every handoff reviewable by another worker.

## Global Constraints

1. Do not change the product shape defined in the spec.
2. Do not turn deterministic workflow steps into agent-driven orchestration.
3. Do not add local mastery or known-word truth tables as first-version core state.
4. Do not process all candidate articles deeply; only process the selected article.
5. Do not introduce pgvector, RAG, or multi-agent runtime orchestration in v1 unless explicitly approved.

## Ownership Model

Each plan owns a disjoint write set.

### Plan 01 ownership

- `pom.xml`
- backend project bootstrap
- DB migration baseline
- candidate-generation entities, repositories, services, scheduler, controllers, tests

### Plan 02 ownership

- selected-article workflow services
- article translation and paragraph persistence
- vocabulary extraction pipeline
- Eudic client
- selected-article APIs and tests

### Plan 03 ownership

- frontend app scaffold
- candidate page
- learning page
- history page
- frontend data clients and component tests

### Plan 04 ownership

- tutor agent service
- article-scoped chat APIs
- chat persistence
- tutor prompt assets
- tutor-specific tests

## Allowed Shared Files

These files may be touched by multiple plans, but only under protocol:

- `README.md`
- root-level workspace docs
- `.gitignore`
- shared API type definitions if created
- integration-test composition files

If a worker needs to touch a shared file, that change must be called out in the handoff note.

## Contract-First Rule

Before parallel work starts, the owning plan must publish or freeze:

- DTOs,
- endpoint shapes,
- status enums,
- table names and key fields,
- error contract for public APIs.

Other agents must consume these contracts rather than reinvent them.

## Change Request Rule

If a worker needs to change another plan’s frozen contract:

1. write a short change request,
2. explain why the current contract blocks delivery,
3. propose the exact delta,
4. identify affected plans,
5. do not implement the contract change until reviewed.

The change request can live in the current PR description or handoff note, but it must be explicit.

## Branching Rule

Recommended branch naming:

- `codex/plan-01-foundation`
- `codex/plan-02-learning-workflow`
- `codex/plan-03-frontend-ui`
- `codex/plan-04-tutor-agent`

One branch per plan. Do not mix unrelated plan work in the same branch.

## Commit Rule

Each commit should:

- map to one plan task,
- keep tests and implementation together,
- avoid “misc cleanup” scope,
- include only owned files unless explicitly noted.

Recommended commit prefixes:

- `feat:`
- `test:`
- `refactor:`
- `docs:`
- `chore:`

## Handoff Contract

Every agent must leave a concise handoff note containing:

1. what changed,
2. files touched,
3. tests run,
4. unresolved risks,
5. contract changes, if any,
6. next recommended step.

Template:

```text
Summary:
- ...

Files:
- path

Verification:
- command -> result

Risks:
- ...

Contract notes:
- none | exact change

Next:
- ...
```

## Review Protocol

Every completed task slice should be reviewed before merge.

Minimum review checklist:

- ownership respected,
- no unauthorized contract drift,
- tests cover the public behavior,
- no agent overreach into deterministic workflow,
- no hidden coupling across plans.

## Integration Protocol

When a plan merges, downstream plans must:

1. rebase or merge latest main,
2. verify their contracts still match,
3. rerun focused tests,
4. update handoff note if adaptation was needed.

## Testing Protocol

### Plan-local verification

Every plan must have its own focused verification commands.

### Merge-gate verification

Before merging a plan branch:

- run plan-local tests,
- run compile/build if applicable,
- ensure no unrelated files changed.

### Final integration verification

After all plans merge:

- backend integration tests,
- frontend build,
- selected article workflow test,
- chat flow smoke test,
- Eudic push mocked integration test.

## Conflict Resolution

If two agents need the same file:

1. stop parallel editing,
2. identify the true owner,
3. move the change to the owner branch or split the file boundary,
4. update the plan docs if the ownership map was wrong.

Do not resolve ownership conflicts by “just both editing carefully”.

## Scope Guardrails

The following are rejected unless explicitly approved:

- adding quiz generation in the initial execution wave if it delays the core flow,
- adding semantic search before article tutor basics work,
- adding mobile-native clients,
- adding extra external integrations beyond Eudic,
- replacing deterministic services with chat prompts.

## Definition of Done Per Plan

A plan is done only when:

- its owned functionality works,
- its tests pass,
- its handoff note is complete,
- its public contracts are documented,
- it does not leave hidden TODOs in core flow files.
