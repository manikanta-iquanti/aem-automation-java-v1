package com.aem.bulkauthoring.docadapt.mapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class SourceMappingLoader {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);
    public static final String MAPPINGS_DIR = "input/source-mappings";
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_-]*");

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

    public static File save(SourceMapping mapping) throws IOException {
        if (mapping == null || mapping.getId() == null || !SAFE_ID.matcher(mapping.getId()).matches()) {
            throw new IllegalArgumentException("Mapping id must be letters, digits, _ or -");
        }
        if (mapping.getTargetTemplate() == null || mapping.getTargetTemplate().isBlank()) {
            throw new IllegalArgumentException("Mapping missing targetTemplate");
        }
        Path dir = Path.of(MAPPINGS_DIR);
        Files.createDirectories(dir);
        File file = dir.resolve(mapping.getId() + ".json").toFile();
        MAPPER.writeValue(file, mapping);
        return file;
    }
}
