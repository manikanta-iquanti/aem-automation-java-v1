# Documentation

Guides for the AEM bulk authoring engine.

## Recommended reading order

1. [Phase 1: Template generation](phase-1-template-generation.md) — blueprint JSON → Word DOCX  
2. [Writing blueprint profiles](writing-blueprint-profiles.md) — how to support a new page type  
3. [Phase 2: Package pipeline](phase-2-package-pipeline.md) — authored DOCX → JSON → installable zip  

## Phase 1

| Doc | Description |
|-----|-------------|
| [Phase 1: Template generation](phase-1-template-generation.md) | Inputs, run steps, checklist for DOCX generation |
| [Writing blueprint profiles](writing-blueprint-profiles.md) | `NormalPageProfile`, `MeridianArticleProfile`, and new profiles |

## Phase 2

| Doc | Description |
|-----|-------------|
| [Phase 2: Package pipeline](phase-2-package-pipeline.md) | Parse articles, update blueprint JSON, build FileVault zip |

## Future / planned features

| Doc | Description |
|-----|-------------|
| [Plan: Client doc → target template](plans/client-doc-to-template.md) | Isolated `docadapt` flow — messy client DOCX → marked template (not implemented yet) |
| [Plan: UI Blueprint Studio](plans/ui-blueprint-studio.md) | Local web UI + JSON profiles — non-tech blueprint setup & runs (not implemented yet) |

Back to the [project README](../README.md).
