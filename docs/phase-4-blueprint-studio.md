# Phase 4: Blueprint Studio

Local web UI for non-technical users to manage blueprints and run Phase 1 / Phase 2 **without writing Java profiles**.

## Run

From the project root:

```bash
mvn -q compile exec:java -Dexec.mainClass="com.aem.bulkauthoring.studio.StudioMain"
```

Open [http://127.0.0.1:8080](http://127.0.0.1:8080). The server binds to localhost only.

CLI `Main` still works for power users; Studio does not replace the engines.

## What it does

| Tab | Purpose |
|-----|---------|
| **Blueprints** | Upload page JSON + FileVault zip, auto-derive package paths, pick authorable fields, save a JSON profile |
| **Generate template** | Phase 1 only — write `input/templates/<id>.docx` |
| **Build package** | Phase 2 only — ingest article DOCX files and write `output/<packageName>.zip` |

Generate and Build stay separate (no one-click chain). Tabs enable/disable from readiness checks.

## JSON profiles

New blueprints are data files under `input/profiles/*.json` (see sample `normal-page.json` / `meridian-article.json`). The registry scans that folder and reloads after Studio saves.

You no longer need a Java `*Profile` class or a `BlueprintProfileRegistry` edit for UI-created blueprints.

## Typical flow

1. In AEM, open the sample page and append `.infinity.json` to the URL; save the JSON.
2. Export the page as a FileVault package (zip).
3. In **Blueprints**, upload JSON + zip, review derived paths, select fields, **Save field selection**.
4. Open **Generate template** → **Generate** → download the DOCX; copy it for authors.
5. When articles are filled, open **Build package**, upload `.docx` files → **Build package** → download the zip → install in AEM Package Manager.

## Settings

**Default package name** (header) is stored in `input/studio-settings.json` and used when creating new blueprints. Override per blueprint under Advanced edit.

## API (local)

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/api/blueprints` | List profiles |
| POST | `/api/blueprints` | Create (multipart: `name`, `json`, `zip`) |
| GET | `/api/blueprints/{id}/components` | Analyzed tree for the picker |
| PUT | `/api/blueprints/{id}/fields` | Save selected fields |
| GET | `/api/blueprints/{id}/readiness` | Generate / Build enablement |
| POST | `/api/blueprints/{id}/generate-template` | Phase 1 |
| POST | `/api/blueprints/{id}/build-package` | Phase 2 (multipart `articles`) |
| GET/PUT | `/api/settings` | Default package name |

## Related

- [Phase 1: Template generation](phase-1-template-generation.md)
- [Phase 2: Package pipeline](phase-2-package-pipeline.md)
- [Writing blueprint profiles](writing-blueprint-profiles.md) — Java approach (optional; Studio uses JSON)
- [Plan: UI Blueprint Studio](plans/ui-blueprint-studio.md)
