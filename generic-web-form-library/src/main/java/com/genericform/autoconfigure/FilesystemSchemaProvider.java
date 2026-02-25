package com.genericform.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genericform.core.FormSchema;
import com.genericform.core.SchemaProvider;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * {@link SchemaProvider} that reads form schemas from a filesystem directory.
 * <p>
 * Activated when {@code genericform.schema.source=filesystem}.
 * Resolves schema files as {@code <filesystemPath>/<formId>.json}.
 * </p>
 */
@Slf4j
public class FilesystemSchemaProvider implements SchemaProvider {

    private final ObjectMapper objectMapper;
    private final Path basePath;

    public FilesystemSchemaProvider(ObjectMapper objectMapper, String filesystemPath) {
        this.objectMapper = objectMapper;
        this.basePath = Paths.get(filesystemPath);
        log.info("FilesystemSchemaProvider initialised with path: {}", basePath.toAbsolutePath());
    }

    @Override
    public Optional<FormSchema> load(String formId) {
        Path schemaFile = basePath.resolve(formId + ".json");

        if (!Files.exists(schemaFile)) {
            log.warn("Schema file not found: {}", schemaFile.toAbsolutePath());
            return Optional.empty();
        }

        try {
            byte[] content = Files.readAllBytes(schemaFile);
            FormSchema schema = objectMapper.readValue(content, FormSchema.class);
            log.info("Loaded form schema from filesystem: {}", schemaFile.toAbsolutePath());
            return Optional.of(schema);
        } catch (IOException e) {
            log.error("Failed to read schema from filesystem: {}", schemaFile.toAbsolutePath(), e);
            return Optional.empty();
        }
    }
}
