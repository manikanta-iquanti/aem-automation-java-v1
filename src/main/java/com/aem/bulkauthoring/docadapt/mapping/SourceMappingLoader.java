package com.aem.bulkauthoring.docadapt.mapping;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class SourceMappingLoader {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    public static final String MAPPINGS_DIR = "input/source-mappings";

    private SourceMappingLoader() {
    }

    public static SourceMapping load(File file) throws IOException {
        SourceMapping mapping = MAPPER.readValue(file, SourceMapping.class);
        if (mapping.getId() == null || mapping.getId().isBlank()) {
            String name = file.getName();
            if (name.toLowerCase(Locale.ROOT).endsWith(".json")) {
                name = name.substring(0, name.length() - 5);
            }
            mapping.setId(name);
        }
        if (mapping.getTargetTemplate() == null || mapping.getTargetTemplate().isBlank()) {
            throw new IllegalArgumentException("Mapping missing targetTemplate: " + file);
        }
        return mapping;
    }

    public static SourceMapping loadById(String id) throws IOException {
        File file = new File(MAPPINGS_DIR, id + ".json");
        if (!file.isFile()) {
            throw new IllegalArgumentException("Mapping not found: " + file.getPath());
        }
        return load(file);
    }

    public static List<String> listIds() throws IOException {
        Path dir = Path.of(MAPPINGS_DIR);
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                    .map(p -> {
                        String name = p.getFileName().toString();
                        return name.substring(0, name.length() - 5);
                    })
                    .sorted()
                    .collect(Collectors.toCollection(ArrayList::new));
        }
    }
}
