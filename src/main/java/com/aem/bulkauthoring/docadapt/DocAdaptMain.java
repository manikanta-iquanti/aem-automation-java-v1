package com.aem.bulkauthoring.docadapt;

/**
 * Isolated CLI entry for client DOCX → filled template adapt.
 *
 * <pre>
 * mvn -q compile exec:java -Dexec.mainClass="com.aem.bulkauthoring.docadapt.DocAdaptMain" \
 *   -Dexec.args="usb-content-hub-normal-page"
 *
 * mvn -q compile exec:java -Dexec.mainClass="com.aem.bulkauthoring.docadapt.DocAdaptMain" \
 *   -Dexec.args="auto normal-page"
 * </pre>
 */
public final class DocAdaptMain {

    public static void main(String[] args) throws Exception {
        DocAdaptService service = new DocAdaptService();
        DocAdaptService.AdaptJobResult result;
        if (args.length > 0 && "auto".equalsIgnoreCase(args[0])) {
            if (args.length < 2) {
                throw new IllegalArgumentException("Usage: auto <template-id> [recipe]");
            }
            String recipe = args.length > 2 ? args[2] : "auto";
            result = service.runAuto(DocAdaptService.resolveTemplate(args[1]), recipe, null);
        } else {
            String mappingId = args.length > 0 ? args[0] : "usb-content-hub-normal-page";
            result = service.run(mappingId, null);
        }
        System.out.println("Adapt job: " + result.jobId);
        System.out.println("Mapping: " + result.mappingId);
        System.out.println("Adapted " + result.reviews.size() + " file(s):");
        for (var review : result.reviews) {
            System.out.println("  " + review.getSourceFile() + " → " + review.getAdaptedFile()
                    + " (" + review.getSlots().size() + " slots)"
                    + (review.getRecipeId() == null ? "" : " recipe=" + review.getRecipeId()));
            for (var slot : review.getSlots()) {
                String preview = slot.getValue();
                if (preview.length() > 80) {
                    preview = preview.substring(0, 80) + "…";
                }
                System.out.println("    [[" + slot.getPath() + "]] = " + preview.replace('\n', ' '));
            }
        }
        System.out.println("Review JSON written under output/adapted-articles/*.review.json");
    }
}
