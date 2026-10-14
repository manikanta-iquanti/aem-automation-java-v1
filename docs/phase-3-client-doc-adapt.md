# Phase 3: Client doc adapt (optional)

Isolated flow that maps raw client DOCX files onto a filled Phase-1 template. Sources may be USB-style tables or normal Word articles (H1/H2/paragraphs/lists). Templates always use AEM `[[/jcr:content/…]]` markers; component names may differ.

Does **not** replace Generate template / Upload articles / Build package.

## Quick start (Studio)

1. Open **Adapt sources**.
2. Mapping: **Auto (detect source + bind to template)**.
3. Pick the target template (from `input/templates/`) and source shape (Auto-detect, `usb-table-form`, or `word-outline`).
4. Upload raw DOCX files or leave empty to use `input/raw-source-docs/`.
5. **Run adapt**. Review the first article: change the dropdown on each marker if needed.
6. **Apply this mapping to remaining**, then **Save mapping** so the next similar batch is one click.
7. Approve → **Send approved to articles** (blueprint selected) → Build package as usual.

Saved mappings live in `input/source-mappings/<id>.json` (recipe + bindings, not hardcoded extract rules). The sample `usb-content-hub-normal-page.json` still works as the old explicit-path form.

## CLI

Legacy mapping file:

```bash
mvn -q compile exec:java -Dexec.mainClass="com.aem.bulkauthoring.docadapt.DocAdaptMain" \
  -Dexec.args="usb-content-hub-normal-page"
```

Auto-detect against any generated template:

```bash
mvn -q compile exec:java -Dexec.mainClass="com.aem.bulkauthoring.docadapt.DocAdaptMain" \
  -Dexec.args="auto normal-page"
```

Optional third argument is the recipe id (`usb-table-form` or `word-outline`).

## How binding works

1. A **source recipe** turns the raw DOCX into a canonical `title` + heading/paragraph/list blocks.
2. A **slot catalog** reads `[[path]]` markers and infers roles from property names (`jcr:title`, `text`/`body`, `level`, `listType`, …).
3. **Dump** is used when the template has a single rich-text slot (normal-page, qq). **Sequential** fills heading/paragraph/list components in order (meridian-style).
4. Teach-once overrides are stored as `bindings` and reused for the rest of the batch.

## USB sections: one text component per Element/Content table

`usb-table-form` sources split the body into several tables whose first row is `Element | Content`. Each such table is a **section**, and the recipe tags its blocks with a 1-based section number (review units `section:1`, `section:2`, …).

When a source has more than one section and no teach-once overrides, the **sections** strategy runs instead of dump/sequential:

- `jcr:title` gets the page title.
- Section 1 goes to the first rich-text marker in the template (for bb-usb, pick the article text module, e.g. `[[/jcr:content/root/responsivegrid/advancedlayout_copy/leftPar/textmodule_copy/text]]`).
- Section *n* goes to `path#n`. The template only needs the one marker; the adapted DOCX gets a copy of that marker block (`[[path#2]]`, `[[path#3]]`, …) stacked directly below it.

At build time (Phase 2), `BlueprintUpdater` reads each `path#n` marker as "clone the component that owns `path`":

- The clone is a sibling named `<component>_n` (or the next free name), e.g. `textmodule_copy_2`.
- It is inserted directly after the previous section's component, so all sections stay together and the remaining components (`fullspancontentblock`, `imagemodule`, …) follow after the last one.
- Instance properties (`jcr:uuid`, `jcr:created*`, `jcr:lastModified*`, `cq:lastModified*`) are dropped from the clone; the field format of `path` is reused.
- Components excluded in the picker are removed afterwards, so exclude the other article text modules you don't want carried over.

Single-section USB docs and `word-outline` sources are unchanged.

A new source family is a new recipe that outputs the same content model. No per-template Java.

See [plans/client-doc-to-template.md](plans/client-doc-to-template.md) for the original v1 mapping language (still loaded when `slots[]` is present).
