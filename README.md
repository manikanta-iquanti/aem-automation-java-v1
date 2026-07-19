# AEM Bulk Authoring

Java tool that turns one authored AEM page (blueprint) into many similar pages by driving content from Word documents and producing an installable FileVault content package.

**Primary use case:** You authored a page with a chosen template and components. Creating 50+ copies by hand in AEM is tedious. This engine generates editable Word templates from the blueprint, ingests filled DOCX files, and builds a package you install via Package Manager / CRXDE.

---

## Documentation

| Guide | Status |
|-------|--------|
| **[Phase 1: Blueprint → Word template](docs/phase-1-template-generation.md)** | Documented and ready to use |
| **[Writing blueprint profiles](docs/writing-blueprint-profiles.md)** | How to onboard new page types |
| [Docs index](docs/README.md) | All guides |

Phase 1 is the supported starting point. Later steps (parse DOCX → update JSON → zip package) exist in code but may still need fixes for new blueprints—prefer the Phase 1 docs until that flow is finalized.

---

## End-to-end pipeline (overview)

```text
┌─────────────────────┐
│ Blueprint JSON      │  one sample page (structure + seed content)
│ (+ optional XML pkg)│
└──────────┬──────────┘
           │  Phase 1 — generate-template
           ▼
┌─────────────────────┐
│ Word template DOCX  │  [[/jcr:content/.../field]] + editable seed text
└──────────┬──────────┘
           │  authors duplicate & fill many DOCXs
           ▼
┌─────────────────────┐
│ input/articles/*.docx│
└──────────┬──────────┘
           │  Phase 2 — parse-document (in progress)
           ▼
┌─────────────────────┐
│ Updated page JSON   │
└──────────┬──────────┘
           │  Phase 3 — package build (in progress)
           ▼
┌─────────────────────┐
│ bulk-articles.zip   │  FileVault package → AEM Package Manager
└─────────────────────┘
```

---

## Project layout

```text
aem-bulk-authoring/
├── README.md
├── docs/                          ← guides (start here for Phase 1)
├── pom.xml                        ← Java 11, Jackson, Apache POI
├── input/
│   ├── blueprint/                 ← page JSON blueprints
│   │   ├── page.json              ← normal page (NormalPageProfile)
│   │   └── page1.json             ← Meridian article (MeridianArticleProfile)
│   ├── blueprint-xml/             ← reference FileVault exports
│   │   ├── normal-page1/
│   │   └── poc-3/
│   ├── templates/                 ← generated DOCX templates
│   └── articles/                  ← authored DOCX inputs (Phase 2)
├── output/                        ← updated JSON, package folder, zip
└── src/main/java/com/aem/bulkauthoring/
    ├── Main.java                  ← MODE + blueprint selection
    ├── analyzer/                  ← JSON → component list
    ├── blueprint/                 ← profiles + registry (Phase 1)
    │   └── profiles/              ← ← add new blueprint profiles here
    ├── generator/                 ← generic DOCX writer
    ├── parser/                    ← DOCX → blocks
    ├── updater/                   ← apply blocks to JSON
    └── packagebuilder/            ← FileVault zip
```

---

## Requirements

- Java 11+
- Maven 3.6+

```bash
mvn -q compile
```

---

## Quick start (Phase 1 only)

Generate a Word template from the normal-page blueprint:

1. In `Main.java` ensure:

```java
private static final String MODE = "generate-template";
private static final String BLUEPRINT_KEY = "normal-page";
private static final File BLUEPRINT =
        new File("input/blueprint/page.json");
```

2. Run:

```bash
mvn -q compile exec:java -Dexec.mainClass="com.aem.bulkauthoring.Main"
```

3. Open:

```text
input/templates/normal-page.docx
```

For the Meridian article blueprint, use `BLUEPRINT_KEY = "meridian-article"` and `input/blueprint/page1.json` → output `input/templates/meridian-article.docx`.

**Full guide:** [docs/phase-1-template-generation.md](docs/phase-1-template-generation.md)

---

## Adding a new blueprint (Phase 1)

Blueprints differ by components and fields. Generic code stays unchanged; you add a **profile**.

1. Put JSON in `input/blueprint/`.
2. Create a class under `src/main/java/com/aem/bulkauthoring/blueprint/profiles/`.
3. Register it in `BlueprintProfileRegistry`.
4. Point `Main` at the new key + file and run `generate-template`.

Worked examples (`NormalPageProfile`, `MeridianArticleProfile`) and a copy-paste skeleton:

→ **[docs/writing-blueprint-profiles.md](docs/writing-blueprint-profiles.md)**

---

## Modes in `Main`

| `MODE` | Purpose |
|--------|---------|
| `generate-template` | Phase 1 — JSON + profile → DOCX under `input/templates/` |
| `parse-document` | Later — parse `input/articles/*.docx`, update blueprint, build zip |

Switch by changing the `MODE` constant in [`Main.java`](src/main/java/com/aem/bulkauthoring/Main.java).

---

## Design notes

- **JSON** is the working format through analyze / update.
- **Profiles** are an allow-list of authorable fields so Word stays clean (no layout/chrome noise).
- **Markers** are JCR paths (`[[/jcr:content/...]]`) so updates can target the correct property in the tree.
- **XML packages** under `input/blueprint-xml/` are the FileVault shape used when building installable zips.

---

## License / project

Internal sample tooling for AEM bulk page authoring (`com.aem:aem-bulk-authoring:1.0.0`).
