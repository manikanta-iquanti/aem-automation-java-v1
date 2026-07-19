# Phase 1: Blueprint JSON → Word template

This guide covers the **first flow** of the bulk-authoring engine: take one authored AEM page exported as JSON (to get this in ur page url replace the .html at the end to .infinity.json), declare which fields authors may edit, and generate a Word (`.docx`) template with path markers.

> Detailed instructions for writing profile classes: [Writing blueprint profiles](writing-blueprint-profiles.md)

---

## What this flow does

```
input/blueprint/<page>.json
        │
        ▼
 BlueprintAnalyzer          discovers components (sling:resourceType) + scalar props
        │
        ▼
 BlueprintTemplateProfile   allow-list: which resource types / fields go into Word
        │
        ▼
 DocumentTemplateGenerator  writes [[jcr-path]] markers + seed values
        │
        ▼
 input/templates/<profile-id>.docx
```

Authors later edit the text under each `[[...]]` marker. Parsing those edits back into JSON / a content package is a later phase.

---

## What you need as input

| Input | Where to put it | Notes |
|-------|-----------------|--------|
| Blueprint JSON | `input/blueprint/` | Export / convert one sample page to JSON (same tree as FileVault). Examples: `page.json`, `page1.json`. |
| Reference XML package (optional for Phase 1) | `input/blueprint-xml/<name>/` | Useful for comparing structure; **not required** to generate the DOCX. |
| Profile class | `src/.../blueprint/profiles/` | Declares editable fields for that blueprint. |
| Registry entry | `BlueprintProfileRegistry` | Maps a string key → profile instance. |
| Main selection | `Main.java` | Set `MODE`, `BLUEPRINT_KEY`, and `BLUEPRINT` file. |

You do **not** need Word documents in `input/articles/` for Phase 1.

---

## How to run Phase 1

1. Open [`Main.java`](../src/main/java/com/aem/bulkauthoring/Main.java).
2. Set:

```java
private static final String MODE = "generate-template";

private static final String BLUEPRINT_KEY = "normal-page";

private static final File BLUEPRINT =
        new File("input/blueprint/page.json");
```

3. From the project root:

```bash
mvn -q compile exec:java -Dexec.mainClass="com.aem.bulkauthoring.Main"
```

4. Open the generated file:

```text
input/templates/normal-page.docx
```

### Built-in combinations

| `BLUEPRINT_KEY` | Blueprint JSON | Output DOCX |
|-----------------|----------------|-------------|
| `normal-page` | `input/blueprint/page.json` | `input/templates/normal-page.docx` |
| `meridian-article` | `input/blueprint/page1.json` | `input/templates/meridian-article.docx` |

---

## What the DOCX looks like

Each editable field is two paragraphs:

1. **Marker** (grey, bold) — do not edit: `[[/jcr:content/root/container/.../property]]`
2. **Value** — seed text from the blueprint (authors replace this)

Example for the normal-page blueprint:

```text
[[/jcr:content/jcr:title]]
Normal Page1

[[/jcr:content/root/container/container/author/fname]]
Pooja

[[/jcr:content/root/container/container/author/lname]]
Bagri

[[/jcr:content/root/container/container/text/text]]
Test heading
...
```

Markers are **path-based**. The updater in later phases matches these same paths onto the JSON tree.

---

## Separation of concerns (what you change vs what you leave alone)

| Layer | Package / class | Change when adding a blueprint? |
|-------|-----------------|----------------------------------|
| Analyzer | `BlueprintAnalyzer` | No — works for any JSON page tree |
| Generator | `DocumentTemplateGenerator` | No — generic DOCX writer |
| **Profile** | `blueprint.profiles.*` | **Yes — write a new class** |
| Registry | `BlueprintProfileRegistry` | **Yes — one `register(...)` line** |
| Entry point | `Main` | **Yes — point KEY + file at your blueprint** |

Profiles are an **allow-list**. Only listed resource types and properties appear in Word. Containers, headers, footers, and technical props (`layout`, `dropCap`, …) stay out unless you add them on purpose.

---

## Adding a new blueprint (checklist)

1. Export a sample page to JSON → `input/blueprint/my-page.json`.
2. Inspect each content node’s `sling:resourceType` and the **authorable** properties (title, body, names, …).
3. Create `MyPageProfile.java` under `blueprint/profiles/` (copy an existing profile).
4. Register it in `BlueprintProfileRegistry`.
5. Set `BLUEPRINT_KEY` / `BLUEPRINT` in `Main`, run `generate-template`.
6. Open `input/templates/<your-profile-id>.docx` and confirm every intended field has a marker.

Step-by-step profile authoring with full examples: **[Writing blueprint profiles](writing-blueprint-profiles.md)**.

---

## Field formats

When declaring a field in a profile, pick a format:

| Helper | Use when | Seed behavior in Word |
|--------|----------|------------------------|
| `EditableField.plain("title")` | Simple strings | Copied as-is |
| `EditableField.html("body")` | Rich text / HTML in JSON | Tags stripped for readability |
| `EditableField.list("items")` | `item0` / `item1` / … children | Seeded as `- line` list from JSON when possible |

Page-level properties use an **absolute** path starting with `/`:

```java
EditableField.plain("/jcr:content/jcr:title")
```

---

## Troubleshooting

| Symptom | Likely cause |
|---------|----------------|
| Empty or almost empty DOCX | Profile does not list the resource types in your JSON, or `BLUEPRINT_KEY` does not match the registered profile |
| Missing field (e.g. hero `eyebrow`) | Field not listed in the profile’s `fieldsFor` map |
| Marker path looks wrong | Node name in JSON differs; path is built from JSON keys under `jcr:content` |
| `Unknown blueprint profile key` | Forgot to `register(...)` in `BlueprintProfileRegistry` |
| Seed value blank but marker present | Property missing on that node in JSON (block is still emitted so authors can fill it) |

---

## Related docs

- [Writing blueprint profiles](writing-blueprint-profiles.md) — how to create `NormalPageProfile`-style classes
- [Project README](../README.md) — full pipeline overview
