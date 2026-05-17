# Static Learning Article Export Design

## Goal

Create a local static archive for each fully processed learning article so the user can reopen or share a bilingual reading copy without rerunning any LLM work.

## Chosen Approach

The application will generate one HTML file after the article reaches `VOCAB_READY`.

- Database entities remain the source of truth.
- The HTML file is an additional artifact, not a replacement storage layer.
- Files are regenerated in place on retries, using a stable path derived from the article id and title.
- Export failures are logged but do not fail the learning workflow, because the export is optional output rather than required pipeline state.

## Alternatives Considered

### Markdown only

Markdown is simpler to diff and maintain, but it provides a weaker reading experience and requires another renderer for polished offline reading.

### HTML and Markdown together

This is flexible, but it introduces two templates and two verification surfaces before the user has a demonstrated need for both formats.

## Architecture

### `LearningArticleExportService`

Responsibilities:

- Build a stable export path under `data/exports/articles/`
- Render article metadata, bilingual paragraphs, and vocabulary items into a self-contained HTML document
- Create parent directories as needed
- Overwrite the same file when the same article is processed again

Dependencies:

- No repository access
- Accepts already loaded `LearningArticleEntity`, `ArticleParagraphEntity`, and `VocabularyItemEntity` values from the workflow
- Uses local filesystem APIs only

### `LearningWorkflowService`

Responsibilities added:

- After paragraph and vocabulary persistence succeeds, invoke `LearningArticleExportService`
- Swallow and log export exceptions so translation and vocabulary results remain successful

## File Format

Each file is stored at:

`data/exports/articles/{learningArticleId}-{slug}.html`

The document contains:

- article title
- source and canonical URL
- one bilingual block per paragraph
- a vocabulary section with word, type, IPA, English definition, Chinese definition, and source sentence

The HTML should be self-contained:

- inline CSS only
- UTF-8 output
- escaped text content

## Error Handling

- If filesystem export fails, the workflow logs `learning.export.failed`
- The article remains `VOCAB_READY`
- Retrying article processing attempts export again and overwrites the same destination when successful

## Testing

- Unit test path generation and HTML rendering in `LearningArticleExportServiceTest`
- Unit test that `LearningWorkflowService` invokes export after vocabulary extraction
- Unit test that export failure does not downgrade workflow success

## Out Of Scope

- Serving static files from the backend
- Adding frontend buttons to open or download exports
- Supporting Markdown export
- Using static exports as an application cache
