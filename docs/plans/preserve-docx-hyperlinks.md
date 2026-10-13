# Plan: Preserve DOCX hyperlinks

> **Status:** Implemented.

Preserve every external Word hyperlink while adapting content, emitting safe HTML anchor tags in headings, paragraphs, and list items without changing the surrounding extraction behavior.

## Scope

Both link storage forms in the current source documents will be supported:

- Normal Word hyperlinks, such as `Section 179` linking to its U.S. Bank URL.
- Legacy `HYPERLINK` field codes, such as `Contact a manufacturing specialist` linking to the equipment-finance contact URL.

Links will be preserved in body headings, paragraphs, and list items. Page titles remain plain text because `jcr:title` is not an HTML field. Internal bookmarks and non-web schemes such as `javascript:` are ignored, while their visible text is retained.

## Data flow

`SourceDocumentNormalizer` currently calls `XWPFParagraph.getText()`, which discards hyperlink relationships before either recipe or the legacy rule engine can see them.

Add a small `FormattedText` model containing ordered plain and linked segments, with `plainText()` and `toHtml()` methods. HTML generation escapes segment text and URL attributes, then emits `<a href="...">visible text</a>`. A shared paragraph reader walks each paragraph in document order:

- A `w:hyperlink` with an external `http`, `https`, or `mailto` relationship becomes one linked segment.
- A `HYPERLINK "..."` field uses only the text between its separator and end markers as the linked text.
- All other text remains a plain segment.

`ParagraphBlock` and `TableCell` will carry this formatted text while continuing to expose plain text for headings, labels, excerpts, and existing comparisons. `ContentUnit`, both source recipes, and `RuleEngineStrategy` will place `formatted.toHtml()` inside their existing `<h2>`, `<p>`, and `<li>` tags. They must not escape that result again.

For example, a paragraph containing the Section 179 link becomes:

```html
<p>… <a href="https://www.usbank.com/.../maximize-deductions-section-179.html">Section 179</a> …</p>
```

## Files

- `src/main/java/com/aem/bulkauthoring/docadapt/extract/SourceDocumentNormalizer.java`
- `src/main/java/com/aem/bulkauthoring/docadapt/model/ParagraphBlock.java`
- `src/main/java/com/aem/bulkauthoring/docadapt/model/TableCell.java`
- `src/main/java/com/aem/bulkauthoring/docadapt/content/ContentUnit.java`
- `src/main/java/com/aem/bulkauthoring/docadapt/recipe/UsbTableFormRecipe.java`
- `src/main/java/com/aem/bulkauthoring/docadapt/recipe/WordOutlineRecipe.java`
- `src/main/java/com/aem/bulkauthoring/docadapt/strategy/RuleEngineStrategy.java`

## Verification

Add focused tests using a generated DOCX for a normal hyperlink and the real `input/raw-source-docs/USB new content hub page1.docx` for both storage forms. Assert the three known URLs and their visible labels survive USB extraction, and that unsafe URLs do not become anchors. Run the existing adapt tests to confirm plain-text behavior stays unchanged.

## Tasks

1. Add formatted paragraph reading for normal and field-code hyperlinks.
2. Render safe anchors through recipes, content units, and legacy rules.
3. Test generated and real USB documents, including unsafe URLs.
