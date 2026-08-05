# Documentation

Guides for the AEM bulk authoring engine.

## Recommended reading order

1. [Phase 4: Blueprint Studio](phase-4-blueprint-studio.md) — local UI (preferred for demos / non-tech)  
2. [Phase 1: Template generation](phase-1-template-generation.md) — blueprint JSON → Word DOCX  
3. [Phase 3: Client doc adapt](phase-3-client-doc-adapt.md) — optional raw client DOCX → filled template  
4. [Phase 2: Package pipeline](phase-2-package-pipeline.md) — authored DOCX → JSON → installable zip  
5. [Writing blueprint profiles](writing-blueprint-profiles.md) — JSON profiles (and legacy Java notes)

## Phase 1

| Doc | Description |
|-----|-------------|
| [Phase 1: Template generation](phase-1-template-generation.md) | Inputs, run steps, checklist for DOCX generation |
| [Writing blueprint profiles](writing-blueprint-profiles.md) | `input/profiles/*.json` allow-lists and package config |

## Phase 2

| Doc | Description |
|-----|-------------|
| [Phase 2: Package pipeline](phase-2-package-pipeline.md) | Parse articles, update blueprint JSON, build FileVault zip |

## Phase 3

| Doc | Description |
|-----|-------------|
| [Phase 3: Client doc adapt](phase-3-client-doc-adapt.md) | Optional isolated adapt: raw client DOCX → filled `[[path]]` template |
| [Plan: Client doc → target template](plans/client-doc-to-template.md) | Design + mapping DSL |

## Phase 4

| Doc | Description |
|-----|-------------|
| [Phase 4: Blueprint Studio](phase-4-blueprint-studio.md) | Local web UI — upload, field picker, generate, adapt, build package |

## Future / planned features

| Doc | Description |
|-----|-------------|
| [Plan: UI Blueprint Studio](plans/ui-blueprint-studio.md) | Design notes for Studio (implemented — see Phase 4 guide) |

Back to the [project README](../README.md).
