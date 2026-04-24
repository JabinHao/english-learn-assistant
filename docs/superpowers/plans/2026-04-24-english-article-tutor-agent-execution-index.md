# English Article Tutor Agent Execution Index

This index splits execution into multiple documents so several agents can work in parallel without stepping on each other.

## Source of Truth

- Spec: [2026-04-24-english-article-tutor-agent-design.md](/Users/jphao/Project/ai-coding/english-learn-assistant/docs/superpowers/specs/2026-04-24-english-article-tutor-agent-design.md)
- Collaboration protocol: [2026-04-24-multi-agent-collaboration-protocol.md](/Users/jphao/Project/ai-coding/english-learn-assistant/docs/superpowers/plans/2026-04-24-multi-agent-collaboration-protocol.md)

## Execution Documents

1. Foundation + candidate generation
   - [2026-04-24-plan-01-foundation-and-candidate-pipeline.md](/Users/jphao/Project/ai-coding/english-learn-assistant/docs/superpowers/plans/2026-04-24-plan-01-foundation-and-candidate-pipeline.md)
2. Learning workflow + Eudic push
   - [2026-04-24-plan-02-learning-workflow-and-eudic.md](/Users/jphao/Project/ai-coding/english-learn-assistant/docs/superpowers/plans/2026-04-24-plan-02-learning-workflow-and-eudic.md)
3. Frontend app + learning UI
   - [2026-04-24-plan-03-frontend-and-learning-ui.md](/Users/jphao/Project/ai-coding/english-learn-assistant/docs/superpowers/plans/2026-04-24-plan-03-frontend-and-learning-ui.md)
4. Tutor agent + chat APIs
   - [2026-04-24-plan-04-tutor-agent-and-chat.md](/Users/jphao/Project/ai-coding/english-learn-assistant/docs/superpowers/plans/2026-04-24-plan-04-tutor-agent-and-chat.md)

## Recommended Order

### Phase A

- Plan 01

This establishes project scaffolding, schema, candidate generation, and the first stable API surface.

### Phase B

- Plan 02
- Plan 03

These can run in parallel once Plan 01 lands or once its contract files are frozen.

### Phase C

- Plan 04

This should start after Plan 02 and Plan 03 define stable article-read and chat request contracts.

## Parallelism Rules

- Plan 01 owns schema and backend foundation.
- Plan 02 owns selected-article processing and Eudic integration.
- Plan 03 owns frontend workspace and UI routes.
- Plan 04 owns tutor agent service and article-scoped chat flow.

No agent should edit another plan’s owned files unless the collaboration protocol explicitly allows it.

## Freeze Points

Before parallel execution starts, freeze these interfaces:

- DB tables for `candidate_*`, `learning_article`, `article_paragraph`, `vocabulary_item`
- candidate list API response
- selected article read model
- chat request/response payloads

Any changes after freeze require protocol review in the collaboration document.

## Success Criteria

- Daily pipeline can generate 3-5 candidates.
- User can select one article for intensive reading.
- Selected article gets translated and vocabulary is pushed to Eudic.
- UI supports candidate selection and article learning.
- Tutor agent can explain and answer questions about the selected article.

## Merge Strategy

- Land Plan 01 first.
- Land Plan 02 and Plan 03 behind stable contracts.
- Land Plan 04 after upstream contracts are merged or explicitly frozen.
- Run final end-to-end verification only after all four plans are merged.
