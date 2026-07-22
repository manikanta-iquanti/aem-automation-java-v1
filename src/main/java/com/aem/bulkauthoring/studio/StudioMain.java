package com.aem.bulkauthoring.studio;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

/**
 * Local Blueprint Studio web UI on {@code http://127.0.0.1:8080}.
 *
 * <pre>
 * mvn -q compile exec:java -Dexec.mainClass="com.aem.bulkauthoring.studio.StudioMain"
 * </pre>
 */
public final class StudioMain {

    public static final int PORT = 8000;

    private StudioMain() {
    }

    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/";
                staticFiles.directory = "/public";
                staticFiles.location = Location.CLASSPATH;
            });
        });

        new StudioApi().register(app);

        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(500).json(java.util.Map.of(
                    "error", e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()
            ));
        });

        app.start("127.0.0.1", PORT);
        System.out.println();
        System.out.println("Blueprint Studio running at http://127.0.0.1:" + PORT);
        System.out.println("Press Ctrl+C to stop.");
    }
}
