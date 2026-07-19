# Phase 2: Word articles → content package

This guide covers the **second flow**: take authored DOCX files (copied from a Phase 1 template), apply their content onto the blueprint JSON, and build an installable FileVault zip.

Related:

- [Phase 1: Template generation](phase-1-template-generation.md)
- [Writing blueprint profiles](writing-blueprint-profiles.md) — field lists + `packageConfig()`

---

## What this flow does

```
input/articles/*.docx
        │
        ▼
 DocumentParser              [[/jcr:content/...]] → path + value blocks
        │
        ▼
 PathFormatIndex             profile FieldFormat per path (PLAIN / HTML / LIST)
        │
        ▼
 BlueprintUpdater            write values onto a copy of the blueprint JSON
        │
        ▼
 output/pages/<name>.json
        │
        ▼
 PackageBuilder              scaffold from profile.packageConfig() + DocView XML
        │
        ▼
 output/<packageName>.zip    → AEM Package Manager
```

The **same profile** used in Phase 1 drives reverse mapping: path markers already identify properties; `FieldFormat` decides how to store them (e.g. wrap HTML for `text` / `body`).

---

## Inputs

| Input | Location | Notes |
|-------|----------|--------|
| Blueprint JSON | `input/blueprint/` | Same file as Phase 1 |
| Profile | `blueprint/profiles/` | Field allow-list + `packageConfig()` |
| Authored DOCX | `input/articles/*.docx` | Copied from `input/templates/<profile-id>.docx` and edited |
| FileVault scaffold | `input/blueprint-xml/<export>/` | Real CRX package export for that page type |

Page names in the package come from the **DOCX filename** (`article1.docx` → page `article1`).

---

## How to run

In [`Main.java`](../src/main/java/com/aem/bulkauthoring/Main.java):

```java
private static final String MODE = "parse-document";
private static final String BLUEPRINT_KEY = "normal-page";
private static final File BLUEPRINT =
        new File("input/blueprint/page.json");
```

```bash
mvn -q compile exec:java -Dexec.mainClass="com.aem.bulkauthoring.Main"
```

Outputs:

| Artifact | Example |
|----------|---------|
| Updated JSON | `output/pages/article1.json`, `article2.json` |
| Package folder | `output/package/` |
| Installable zip | `output/bulk-normal-pages.zip` (name from profile) |

---

## Built-in combinations

| Profile key | JSON | XML scaffold | Zip name |
|-------------|------|--------------|----------|
| `normal-page` | `page.json` | `input/blueprint-xml/normal-page1` | `bulk-normal-pages.zip` |
| `meridian-article` | `page1.json` | `input/blueprint-xml/poc-3` | `bulk-articles.zip` |

---

## Separation of concerns

| Layer | Change for a new blueprint? |
|-------|------------------------------|
| `DocumentParser` | No |
| `PathFormatIndex` | No — built from profile fields |
| `BlueprintUpdater` | No — uses formats map |
| `PackageBuilder` / writers | No — driven by `packageConfig()` |
| **Profile field list** | Yes (Phase 1) |
| **`packageConfig()`** | **Yes — scaffold dir, content parent, sample page, package name** |

### `packageConfig()` example

```java
@Override
public BlueprintPackageConfig packageConfig() {
    return new BlueprintPackageConfig(
            new File("input/blueprint-xml/normal-page1"),  // vault export
            "/content/my-aem-site53/us/en/articles",       // parent path
            "normal-page1",                                // sample folder to remove
            "bulk-normal-pages"                            // zip name
    );
}
```

---

## Field formats on the reverse path

| Format | Stored in JSON as |
|--------|-------------------|
| `PLAIN` | Exact DOCX text |
| `HTML` | If not already HTML: paragraphs from blank lines, soft breaks → `<br/>`, wrapped in `<p>` |
| `LIST` | Lines (`- item`) written into `item0` / `item1` / … `text` children |

You do **not** write a second field map for Phase 2 — the Phase 1 profile is enough.

---

## Checklist for a new blueprint

1. Complete Phase 1 (JSON + profile + template).
2. Export a FileVault package for that page type → `input/blueprint-xml/<name>/`.
3. Implement `packageConfig()` on the profile.
4. Copy the generated template to `input/articles/` as `article1.docx`, `article2.docx`, … and edit values under markers (do not change marker lines).
5. Set `MODE = parse-document`, `BLUEPRINT_KEY`, and blueprint file in `Main`.
6. Run and install `output/<packageName>.zip`.

---

## Troubleshooting

| Symptom | Likely cause |
|---------|----------------|
| Content still matches blueprint seed | Markers edited/removed, or wrong blueprint JSON vs DOCX template |
| `text` / `body` not HTML | Profile field not marked `EditableField.html(...)` |
| Zip still has Meridian Vietnam page | Profile still pointing `packageConfig` at `poc-3` |
| Filter paths wrong | `contentParentPath` in `packageConfig` incorrect |
| Soft-break lines flattened | Rebuild template with current generator and re-copy articles if needed |

---

## Related docs

- [Phase 1: Template generation](phase-1-template-generation.md)
- [Writing blueprint profiles](writing-blueprint-profiles.md) — fields + `packageConfig()`
- [Project README](../README.md) — quick start for both phases
