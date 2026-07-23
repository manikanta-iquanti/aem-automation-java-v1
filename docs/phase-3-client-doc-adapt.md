# Phase 3: Client doc adapt (optional)

Isolated flow that maps raw client DOCX files onto a filled Phase-1 template using declarative source-mapping JSON. Does **not** replace Generate template / Upload articles / Build package.

## Quick start (CLI)

1. Put raw sources in `input/raw-source-docs/` (sample: USB Content Hub form).
2. Ensure target template exists (`input/templates/normal-page.docx`).
3. Use or edit a mapping under `input/source-mappings/` (sample: `usb-content-hub-normal-page.json`).
4. Run:

```bash
mvn -q compile exec:java -Dexec.mainClass="com.aem.bulkauthoring.docadapt.DocAdaptMain" \
  -Dexec.args="usb-content-hub-normal-page"
```

5. Review `output/adapted-articles/*.docx` and `*.review.json`.
6. Copy approved DOCX into `input/articles/<profileId>/` and run Phase 2 as usual.

## Studio

Open **Adapt sources** tab → pick mapping → Run adapt → side-by-side review → approve → **Send approved to articles** (requires a blueprint selected). Existing tabs stay independent.

## New source family

Add `input/source-mappings/<id>.json` with `targetTemplate` + `slots[].extract` rules. No Java changes unless you need a new extract `type`. See [plans/client-doc-to-template.md](plans/client-doc-to-template.md).
