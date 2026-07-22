# AEM Bulk Authoring

Java tool that turns one authored AEM page (blueprint) into many similar pages by driving content from Word documents and producing an installable FileVault content package.

**Primary use case:** You authored a page with a chosen template and components. Creating 50+ copies by hand in AEM is tedious. This engine generates editable Word templates from the blueprint, ingests filled DOCX files, and builds a package you install via Package Manager / CRXDE.

---

## Documentation

| Guide | Description |
|-------|-------------|
| **[Phase 1: Blueprint → Word template](docs/phase-1-template-generation.md)** | Generate editable DOCX from blueprint JSON |
| **[Phase 2: DOCX → content package](docs/phase-2-package-pipeline.md)** | Parse authored articles → JSON → installable zip |
| **[Writing blueprint profiles](docs/writing-blueprint-profiles.md)** | Add support for a new page type / template |
| [Plan: Client doc → template](docs/plans/client-doc-to-template.md) | Future isolated adapt flow (not implemented) |
| [Plan: UI Blueprint Studio](docs/plans/ui-blueprint-studio.md) | Future local UI for non-tech setup (not implemented) |
| [Docs index](docs/README.md) | All guides |

---

## End-to-end pipeline

```text
┌─────────────────────┐
│ Blueprint JSON      │  one sample page (structure + seed content)
│ + FileVault XML pkg │  reference export for package scaffold
└──────────┬──────────┘
           │  Phase 1 — generate-template
           ▼
┌─────────────────────┐
│ Word template DOCX  │  [[/jcr:content/.../field]] + editable seed text
│ input/templates/    │
└──────────┬──────────┘
           │  authors copy template → fill many DOCXs
           ▼
┌─────────────────────┐
│ input/articles/*.docx│
└──────────┬──────────┘
           │  Phase 2 — parse-document
           ▼
┌─────────────────────┐
│ output/pages/*.json │  updated blueprints
│ output/<name>.zip   │  FileVault package → AEM Package Manager
└─────────────────────┘
```

Both phases use the **same blueprint profile**: field allow-list for Word, `FieldFormat` for reverse mapping, and `packageConfig()` for the zip scaffold.

---

## Project layout

```text
aem-bulk-authoring/
├── README.md
├── docs/
│   ├── README.md
│   ├── phase-1-template-generation.md
│   ├── phase-2-package-pipeline.md
│   └── writing-blueprint-profiles.md
├── pom.xml                        ← Java 11, Jackson, Apache POI
├── input/
│   ├── blueprint/                 ← page JSON blueprints
│   │   ├── page.json              ← normal page (NormalPageProfile)
│   │   └── page1.json             ← Meridian article (MeridianArticleProfile)
│   ├── blueprint-xml/             ← FileVault exports (package scaffolds)
│   │   ├── normal-page1/
│   │   └── poc-3/
│   ├── templates/                 ← generated DOCX templates (Phase 1)
│   └── articles/                  ← authored DOCX inputs (Phase 2)
├── output/
│   ├── pages/                     ← updated JSON per article
│   ├── package/                   ← unzipped FileVault tree
│   └── *.zip                      ← installable content package
└── src/main/java/com/aem/bulkauthoring/
    ├── Main.java                  ← MODE + BLUEPRINT_KEY + blueprint file
    ├── analyzer/                  ← JSON → component list
    ├── blueprint/                 ← profiles, registry, package config, path formats
    │   └── profiles/              ← add new blueprint profiles here
    ├── generator/                 ← generic DOCX writer
    ├── parser/                    ← DOCX → path/value blocks
    ├── updater/                   ← apply blocks onto JSON
    └── packagebuilder/            ← DocView XML + FileVault zip
```

---

## Requirements

- Java 11+
- Maven 3.6+

```bash
mvn -q compile
```

---

## Quick start

### Phase 1 — generate a Word template

In [`Main.java`](src/main/java/com/aem/bulkauthoring/Main.java):

```java
private static final String MODE = "generate-template";
private static final String BLUEPRINT_KEY = "normal-page";
private static final File BLUEPRINT =
        new File("input/blueprint/page.json");
```

```bash
mvn -q compile exec:java -Dexec.mainClass="com.aem.bulkauthoring.Main"
```

Output: `input/templates/normal-page.docx`

For Meridian: `BLUEPRINT_KEY = "meridian-article"` and `input/blueprint/page1.json` → `input/templates/meridian-article.docx`.

Full guide: [docs/phase-1-template-generation.md](docs/phase-1-template-generation.md)

### Phase 2 — build a content package from authored DOCX

1. Copy the template into `input/articles/` as `article1.docx`, `article2.docx`, … and edit values **under** the `[[...]]` markers (do not change the marker lines).
2. In `Main.java`:

```java
private static final String MODE = "parse-document";
private static final String BLUEPRINT_KEY = "normal-page";
private static final File BLUEPRINT =
        new File("input/blueprint/page.json");
```

3. Run the same Maven command as above.

Outputs:

| Artifact | Example |
|----------|---------|
| Updated JSON | `output/pages/article1.json`, `article2.json` |
| Installable zip | `output/bulk-normal-pages.zip` |

Install the zip in AEM Package Manager. Pages are created under the parent path from the profile’s `packageConfig()` (for normal-page: `/content/my-aem-site53/us/en/articles/article1`, …).

Full guide: [docs/phase-2-package-pipeline.md](docs/phase-2-package-pipeline.md)

---

## Built-in blueprints

| Profile key | JSON | XML scaffold | Template DOCX | Zip name |
|-------------|------|--------------|---------------|----------|
| `normal-page` | `input/blueprint/page.json` | `input/blueprint-xml/normal-page1` | `normal-page.docx` | `bulk-normal-pages.zip` |
| `meridian-article` | `input/blueprint/page1.json` | `input/blueprint-xml/poc-3` | `meridian-article.docx` | `bulk-articles.zip` |

---

## Adding a new blueprint

Generic analyzer / generator / parser / updater / package builder stay unchanged. You add a **profile**.

1. Put JSON in `input/blueprint/`.
2. Export a FileVault package to `input/blueprint-xml/<name>/`.
3. Create `src/.../blueprint/profiles/MyProfile.java` with:
   - editable fields (`fieldsFor` / `pageFields`)
   - `packageConfig()` (scaffold dir, content parent, sample page name, zip name)
4. Register it in `BlueprintProfileRegistry`.
5. Set `BLUEPRINT_KEY` + blueprint file in `Main`, run Phase 1 then Phase 2.

Worked examples and a copy-paste skeleton:

→ **[docs/writing-blueprint-profiles.md](docs/writing-blueprint-profiles.md)**

---

## Modes in `Main`

| `MODE` | Purpose |
|--------|---------|
| `generate-template` | Phase 1 — JSON + profile → DOCX under `input/templates/` |
| `parse-document` | Phase 2 — parse `input/articles/*.docx`, update JSON, build zip |

Switch by changing the `MODE` constant in [`Main.java`](src/main/java/com/aem/bulkauthoring/Main.java). Keep `BLUEPRINT_KEY` and the blueprint file aligned with the articles you are processing.

---

## Design notes

- **JSON** is the working format through analyze / update; **XML** is produced only for the installable package.
- **Profiles** are an allow-list of authorable fields (Word stays free of layout/chrome noise).
- **Markers** are JCR paths (`[[/jcr:content/...]]`) so Phase 2 can target the correct property.
- **`FieldFormat`** (`PLAIN` / `HTML` / `LIST`) is declared once on the profile and used for both template seeding and JSON write-back.
- **`packageConfig()`** points at the FileVault export used as the zip scaffold and sets install paths / package name.

---

## License / project

Internal sample tooling for AEM bulk page authoring (`com.aem:aem-bulk-authoring:1.0.0`).
