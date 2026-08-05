# Writing blueprint profiles

A **profile** tells the template engine which parts of a blueprint JSON page should become editable blocks in Word. Every new page type / template gets its own profile.

**Preferred:** create profiles as JSON under `input/profiles/` (or use [Blueprint Studio](phase-4-blueprint-studio.md)). Samples:

- [`input/profiles/normal-page.json`](../input/profiles/normal-page.json) — simple page (`page.json`)
- [`input/profiles/meridian-article.json`](../input/profiles/meridian-article.json) — Meridian article (`page1.json`)

Loaded by [`JsonBlueprintProfile`](../src/main/java/com/aem/bulkauthoring/blueprint/JsonBlueprintProfile.java) via [`BlueprintProfileRegistry`](../src/main/java/com/aem/bulkauthoring/blueprint/BlueprintProfileRegistry.java).

---

## Interface contract

Profiles implement [`BlueprintTemplateProfile`](../src/main/java/com/aem/bulkauthoring/blueprint/BlueprintTemplateProfile.java):

```java
public interface BlueprintTemplateProfile {

    /** Stable key used in Main + registry + output filename. */
    String id();

    /** Path to the blueprint infinity JSON. */
    File blueprintJson();

    /** Editable fields for one sling:resourceType (empty = skip). */
    List<EditableField> fieldsFor(String resourceType);

    /** Optional page-level absolute paths (default: none). */
    default List<EditableField> pageFields() { ... }

    BlueprintPackageConfig packageConfig();
}
```

### `EditableField` helpers

| Method | Meaning |
|--------|---------|
| `EditableField.plain("fname")` | Plain text property on the component |
| `EditableField.html("text")` | HTML/richtext → stripped in the DOCX seed |
| `EditableField.list("items")` | Nested `item0`, `item1`, … under that child node |
| `EditableField.plain("/jcr:content/jcr:title")` | Absolute page property (starts with `/`) |

Relative properties become markers like:

```text
[[/jcr:content/root/container/mig_hero/title]]
```

Absolute properties use the path as-is:

```text
[[/jcr:content/jcr:title]]
```

---

## How to discover what to put in a profile

Open your blueprint JSON (example: `input/blueprint/page.json`) and find content nodes:

```json
"author": {
  "fname": "Pooja",
  "lname": "Bagri",
  "professor": "true",
  "sling:resourceType": "my-aem-site53/components/author"
}
```

For the profile you need:

1. **Resource type string** — exact `sling:resourceType` value  
2. **Property names** — only the ones authors should change (`fname`, `lname`, …)  
3. **Skip** structural nodes (`container`, `page`, chrome header/footer) by simply not listing them

Do **not** put every JSON key in the profile. Skip technical flags unless you really want them in Word (`layout`, `textIsRich`, `dropCap`, UUIDs, …).

---

## Example 1 — Normal page (existing)

Blueprint: `input/blueprint/page.json`  
Profile: `NormalPageProfile`  
Id: `normal-page`

```java
package com.aem.bulkauthoring.blueprint.profiles;

import com.aem.bulkauthoring.blueprint.BlueprintTemplateProfile;
import com.aem.bulkauthoring.blueprint.EditableField;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NormalPageProfile implements BlueprintTemplateProfile {

    private static final String AUTHOR = "my-aem-site53/components/author";
    private static final String TEXT   = "my-aem-site53/components/text";

    private final Map<String, List<EditableField>> byResourceType = new HashMap<>();

    public NormalPageProfile() {
        byResourceType.put(AUTHOR, Arrays.asList(
                EditableField.plain("fname"),
                EditableField.plain("lname"),
                EditableField.plain("professor")
        ));
        byResourceType.put(TEXT, Collections.singletonList(
                EditableField.html("text")
        ));
        // title component exists in JSON but has no authorable props yet → omit
    }

    @Override
    public String id() {
        return "normal-page";
    }

    @Override
    public List<EditableField> fieldsFor(String resourceType) {
        return byResourceType.getOrDefault(resourceType, Collections.emptyList());
    }

    @Override
    public List<EditableField> pageFields() {
        return Collections.singletonList(
                EditableField.plain("/jcr:content/jcr:title")
        );
    }
}
```

**Resulting markers (among others):**

| Marker | Source |
|--------|--------|
| `[[/jcr:content/jcr:title]]` | `pageFields()` |
| `[[.../author/fname]]` | author component |
| `[[.../author/lname]]` | author component |
| `[[.../text/text]]` | text component (HTML stripped in seed) |

---

## Example 2 — Meridian article (existing)

Blueprint: `input/blueprint/page1.json`  
Profile: `MeridianArticleProfile`  
Id: `meridian-article`

Hero mapping (abbreviated — see the full class in source):

```java
private static final String HERO =
        "my-aem-site53/components/meridian-article-hero";

byResourceType.put(HERO, Arrays.asList(
        EditableField.plain("title"),
        EditableField.plain("dek"),
        EditableField.plain("eyebrow"),
        EditableField.plain("readTime"),
        EditableField.plain("authorName"),
        EditableField.plain("publishDateText"),
        EditableField.plain("image"),
        EditableField.plain("imageAlt")
));

byResourceType.put(
        "my-aem-site53/components/meridian-article-paragraph",
        Collections.singletonList(EditableField.html("body")));

byResourceType.put(
        "my-aem-site53/components/meridian-article-list",
        Collections.singletonList(EditableField.list("items")));
```

**Intentionally omitted** (for now): jump-link nested `links/itemN`, header/footer chrome. Add them later the same way if needed.

---

## Example 3 — Adding a brand-new blueprint

Suppose you exported `input/blueprint/product-landing.json` with:

```json
"hero_banner": {
  "headline": "Summer sale",
  "subhead": "Up to 40% off",
  "ctaLabel": "Shop now",
  "sling:resourceType": "my-aem-site53/components/hero-banner"
},
"feature": {
  "title": "Free shipping",
  "description": "<p>On orders over $50</p>",
  "sling:resourceType": "my-aem-site53/components/feature-card"
}
```

### Step A — Create the profile class

Create:

`src/main/java/com/aem/bulkauthoring/blueprint/profiles/ProductLandingProfile.java`

```java
package com.aem.bulkauthoring.blueprint.profiles;

import com.aem.bulkauthoring.blueprint.BlueprintTemplateProfile;
import com.aem.bulkauthoring.blueprint.EditableField;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Editable fields for {@code input/blueprint/product-landing.json}.
 */
public class ProductLandingProfile implements BlueprintTemplateProfile {

    private static final String HERO_BANNER =
            "my-aem-site53/components/hero-banner";
    private static final String FEATURE_CARD =
            "my-aem-site53/components/feature-card";

    private final Map<String, List<EditableField>> byResourceType = new HashMap<>();

    public ProductLandingProfile() {
        byResourceType.put(HERO_BANNER, Arrays.asList(
                EditableField.plain("headline"),
                EditableField.plain("subhead"),
                EditableField.plain("ctaLabel")
        ));
        byResourceType.put(FEATURE_CARD, Arrays.asList(
                EditableField.plain("title"),
                EditableField.html("description")
        ));
    }

    @Override
    public String id() {
        return "product-landing";
    }

    @Override
    public List<EditableField> fieldsFor(String resourceType) {
        return byResourceType.getOrDefault(resourceType, Collections.emptyList());
    }

    @Override
    public List<EditableField> pageFields() {
        return Collections.singletonList(
                EditableField.plain("/jcr:content/jcr:title")
        );
    }
}
```

### Step B — Drop the JSON profile

Save as `input/profiles/product-landing.json`. The registry scans that folder on startup and on `reload()` (Studio calls reload after save). No Java registration edit needed.

### Step C — Point Main at it (CLI only)

```java
private static final String MODE = "generate-template";
private static final String BLUEPRINT_KEY = "product-landing";
```

### Step D — Generate

```bash
mvn -q compile exec:java -Dexec.mainClass="com.aem.bulkauthoring.Main"
```

Output:

```text
input/templates/product-landing.docx
```

Prefer [Blueprint Studio](phase-4-blueprint-studio.md) for upload + field picker without hand-editing JSON.

---

## Recommended profile skeleton (copy-paste)

```java
package com.aem.bulkauthoring.blueprint.profiles;

import com.aem.bulkauthoring.blueprint.BlueprintPackageConfig;
import com.aem.bulkauthoring.blueprint.BlueprintTemplateProfile;
import com.aem.bulkauthoring.blueprint.EditableField;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyBlueprintProfile implements BlueprintTemplateProfile {

    private static final String MY_COMPONENT =
            "my-aem-site53/components/REPLACE_ME";

    private final Map<String, List<EditableField>> byResourceType = new HashMap<>();

    public MyBlueprintProfile() {
        byResourceType.put(MY_COMPONENT, Arrays.asList(
                EditableField.plain("propertyOne"),
                EditableField.html("richProperty")
                // EditableField.list("items")
        ));
    }

    @Override
    public String id() {
        return "my-blueprint"; // registry key + DOCX filename stem
    }

    @Override
    public List<EditableField> fieldsFor(String resourceType) {
        return byResourceType.getOrDefault(resourceType, Collections.emptyList());
    }

    @Override
    public List<EditableField> pageFields() {
        return Collections.singletonList(
                EditableField.plain("/jcr:content/jcr:title")
        );
    }

    /** Required for Phase 2 (content package). */
    @Override
    public BlueprintPackageConfig packageConfig() {
        return new BlueprintPackageConfig(
                new File("input/blueprint-xml/my-export"),
                "/content/my-aem-site53/us/en/articles",
                "sample-page-name-in-export",
                "bulk-my-blueprint"
        );
    }
}
```

Then:

1. `register(new MyBlueprintProfile());` in the registry  
2. Set `BLUEPRINT_KEY` / `BLUEPRINT` in `Main`  
3. Run `generate-template` (Phase 1), then after authors fill DOCX files run `parse-document` (Phase 2)

---

## Rules of thumb

1. **One profile per blueprint shape** (same components / fields). Many pages that share one template share one profile.
2. **`id()` must be unique** and match what you pass as `BLUEPRINT_KEY` (or at least what you register under — today the registry uses `profile.id()` as the key).
3. **Resource type strings must match JSON exactly** (including site prefix).
4. **List fields** use the child node name that holds `item0`, `item1`, … (`items`, `tags`, …).
5. **Prefer fewer fields** in Word — only what content authors change per article/page.
6. If a component appears many times in the JSON (e.g. several paragraphs), one profile entry covers **all** instances; the generator emits a marker for each occurrence with the correct path.

---

## Where files live

```text
input/profiles/
├── normal-page.json               ← sample JSON profile
├── meridian-article.json
└── your-new-profile.json          ← add new profiles here

src/main/java/com/aem/bulkauthoring/blueprint/
├── BlueprintTemplateProfile.java  ← interface
├── JsonBlueprintProfile.java      ← loads input/profiles/*.json
├── BlueprintPackageConfig.java    ← vault scaffold + install paths (Phase 2)
├── PathFormatIndex.java           ← path → FieldFormat for JSON write-back
├── EditableField.java
├── FieldFormat.java               ← PLAIN / HTML / LIST
└── BlueprintProfileRegistry.java  ← scans input/profiles/, reload() after UI save
```

---

## Related docs

- [Phase 4: Blueprint Studio](phase-4-blueprint-studio.md) — preferred non-tech path
- [Phase 1: Template generation](phase-1-template-generation.md) — inputs, run steps, troubleshooting
- [Phase 2: Package pipeline](phase-2-package-pipeline.md) — DOCX → JSON → content package
- [Project README](../README.md)
