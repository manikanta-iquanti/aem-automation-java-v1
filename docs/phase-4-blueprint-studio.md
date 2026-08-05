# Phase 4: Blueprint Studio

Local web UI for non-technical users to manage blueprints and run Phase 1 / Phase 2 **without writing Java profiles**.

## Run

From the project root:

```bash
mvn -q compile exec:java -Dexec.mainClass="com.aem.bulkauthoring.studio.StudioMain"
```

Open [http://127.0.0.1:8000](http://127.0.0.1:8000). The server binds to localhost only.

CLI `Main` still works for power users; Studio does not replace the engines.

## What it does

| Tab | Purpose |
|-----|---------|
| **Blueprints** | Create from upload **or** local AEM fetch, derive package paths, pick authorable fields, save a JSON profile |
| **Generate template** | Phase 1 only — write `input/templates/<id>.docx` (toggle in Settings) |
| **Adapt sources** | Optional DocAdapt flow — map raw DOCX onto a template (toggle in Settings) |
| **Build package** | Phase 2 — ingest article DOCX files and write `output/<packageName>.zip` |

Generate and Build stay separate (no one-click chain). Tabs enable/disable from readiness checks. Optional tabs can be hidden via **Settings**.

## Settings

Open the **gear** in the header. Preferences persist in `input/studio-settings.json`.

| Setting | Daily default | Purpose |
|---------|---------------|---------|
| Show Generate template | off | Hide Phase 1 after templates exist |
| Show Adapt sources | off | Hide DocAdapt when sources are already converted |
| Show create / upload | on | Hide create section when not adding blueprints |
| Show help text | off | Restore instructional copy / readiness prose |
| Default package name | `bulk-content` | Seed for new blueprints (override per blueprint under Advanced edit) |
| AEM base URL | `http://localhost:4502` | Local Author host for **Fetch from AEM** |
| AEM username / password | `admin` / `admin` | Basic Auth for Author (local Studio only) |

**Presets**

- **Daily authoring** — Generate off, Adapt off, create on, help off (focused Build workflow)
- **Full setup** — all feature toggles and help on

## Create blueprint modes

On **Blueprints → 1. Create blueprint**:

| Mode | Input | Behavior |
|------|-------|----------|
| **Upload files** | JSON file or paste + FileVault zip | Same as before → `POST /api/blueprints` |
| **Fetch from AEM** | Page URL or content path | Studio GETs `{path}.infinity.json` and builds a Package Manager zip filtered to that page → `POST /api/blueprints/from-aem` |

Example page URL: `http://localhost:4502/content/my-aem-site53/us/en/articles/normal-page1.html`  
Paths like `/content/.../page` also work. Credentials come from Settings (local Author only; AEMaaCS tokens are out of scope).

Fetched artifacts land in the same folders as upload: `input/blueprint/<id>.json`, `input/blueprint-xml/<id>/`, `input/profiles/<id>.json`.

## JSON profiles

New blueprints are data files under `input/profiles/*.json` (see sample `normal-page.json` / `meridian-article.json`). The registry scans that folder and reloads after Studio saves.

You no longer need a Java `*Profile` class or a `BlueprintProfileRegistry` edit for UI-created blueprints.

## Typical flow

**First-time setup** — use preset **Full setup**, then:

1. Ensure Settings has your local AEM Author URL and credentials.
2. In **Blueprints**, choose **Fetch from AEM**, paste the page URL, **Fetch & derive** (or use **Upload files** with manual JSON + zip).
3. Review derived paths, select fields, **Save field selection**.
4. Open **Generate template** → **Generate** → download the DOCX; copy it for authors.
5. Optionally use **Adapt sources** for client DOCX → approve → send to articles.

**Day-to-day** — use preset **Daily authoring**, then:

1. Select a blueprint.
2. Open **Build package**, upload `.docx` articles if needed → **Build package** → download the zip → install in AEM Package Manager.

## API (local)

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/api/blueprints` | List profiles |
| POST | `/api/blueprints` | Create (multipart: `name`, `json` or `jsonText`, `zip`) |
| POST | `/api/blueprints/from-aem` | Create from Author (`name`, `pageUrl`) using Settings credentials |
| GET | `/api/blueprints/{id}/components` | Analyzed tree for the picker |
| PUT | `/api/blueprints/{id}/fields` | Save selected fields |
| GET | `/api/blueprints/{id}/readiness` | Generate / Build enablement |
| POST | `/api/blueprints/{id}/generate-template` | Phase 1 |
| POST | `/api/blueprints/{id}/build-package` | Phase 2 (multipart `articles`) |
| GET/PUT | `/api/settings` | Package name, UI flags, AEM credentials |
| GET | `/api/adapt/mappings` | Source-mapping list |
| POST | `/api/adapt/run` | Run DocAdapt |
| POST | `/api/adapt/send-to-articles` | Copy approved adapted DOCX into articles |

## Related

- [Phase 1: Template generation](phase-1-template-generation.md)
- [Phase 2: Package pipeline](phase-2-package-pipeline.md)
- [Writing blueprint profiles](writing-blueprint-profiles.md) — Java approach (optional; Studio uses JSON)
- [Plan: UI Blueprint Studio](plans/ui-blueprint-studio.md)
