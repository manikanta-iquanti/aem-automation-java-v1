# Writing blueprint profiles

A **profile** tells the template engine which parts of a blueprint JSON page should become editable blocks in Word. Every new page type / template gets its own profile class.

Reference implementations:

- [`NormalPageProfile.java`](../src/main/java/com/aem/bulkauthoring/blueprint/profiles/NormalPageProfile.java) — simple page (`page.json`)
- [`MeridianArticleProfile.java`](../src/main/java/com/aem/bulkauthoring/blueprint/profiles/MeridianArticleProfile.java) — Meridian article (`page1.json`)

---

## Interface contract

Implement [`BlueprintTemplateProfile`](../src/main/java/com/aem/bulkauthoring/blueprint/BlueprintTemplateProfile.java):

```java
public interface BlueprintTemplateProfile {

    /** Stable key used in Main + registry + output filename. */
    String id();

    /** Editable fields for one sling:resourceType (empty = skip). */
    List<EditableField> fieldsFor(String resourceType);

    /** Optional page-level absolute paths (default: none). */
    default List<EditableField> pageFields() { ... }
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

### Step B — Register the profile

Edit [`BlueprintProfileRegistry.java`](../src/main/java/com/aem/bulkauthoring/blueprint/BlueprintProfileRegistry.java):

```java
static {
    register(new NormalPageProfile());
    register(new MeridianArticleProfile());
    register(new ProductLandingProfile());  // add this
}
```

### Step C — Point Main at it

```java
private static final String MODE = "generate-template";
private static final String BLUEPRINT_KEY = "product-landing";
private static final File BLUEPRINT =
        new File("input/blueprint/product-landing.json");
```

### Step D — Generate

```bash
mvn -q compile exec:java -Dexec.mainClass="com.aem.bulkauthoring.Main"
```

Output:

```text
input/templates/product-landing.docx
```

---

## Recommended profile skeleton (copy-paste)

```java
package com.aem.bulkauthoring.blueprint.profiles;

import com.aem.bulkauthoring.blueprint.BlueprintTemplateProfile;
import com.aem.bulkauthoring.blueprint.EditableField;

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
        return "my-blueprint"; // used as DOCX filename stem
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

Then:

1. `register(new MyBlueprintProfile());` in the registry  
2. Set `BLUEPRINT_KEY` / `BLUEPRINT` in `Main`  
3. Run `generate-template`

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
src/main/java/com/aem/bulkauthoring/blueprint/
├── BlueprintTemplateProfile.java      ← interface + developer checklist (Javadoc)
├── EditableField.java
├── FieldFormat.java
├── BlueprintProfileRegistry.java      ← register every profile here
└── profiles/
    ├── NormalPageProfile.java
    ├── MeridianArticleProfile.java
    └── YourNewProfile.java            ← add new profiles here
```

---

## Related docs

- [Phase 1: Template generation](phase-1-template-generation.md) — inputs, run steps, troubleshooting
- [Project README](../README.md)
