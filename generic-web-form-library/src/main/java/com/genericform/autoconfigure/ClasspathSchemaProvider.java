package com.genericform.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genericform.core.FormSchema;
import com.genericform.core.SchemaProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * Default {@link SchemaProvider} that reads form schemas from the classpath.
 * <p>
 * Looks for JSON files under the configured prefix (default: {@code forms/}).
 * For example, {@code load("survey")} resolves to
 * {@code classpath:forms/survey.json}.
 * </p>
 */
@Slf4j
public class ClasspathSchemaProvider implements SchemaProvider {

    private final ObjectMapper objectMapper;
    private final String classpathPrefix;

    public ClasspathSchemaProvider(ObjectMapper objectMapper, String classpathPrefix) {
        this.objectMapper = objectMapper;
        this.classpathPrefix = classpathPrefix != null ? classpathPrefix : "forms/";
    }

    @Override
    public Optional<FormSchema> load(String formId) {
        String resourcePath = classpathPrefix + formId + ".json";
        ClassPathResource resource = new ClassPathResource(resourcePath);

        if (!resource.exists()) {
            log.warn("Schema not found on classpath: {}", resourcePath);
            return Optional.empty();
        }

        try (InputStream is = resource.getInputStream()) {
            FormSchema schema = objectMapper.readValue(is, FormSchema.class);
            log.info("Loaded form schema from classpath: {}", resourcePath);
            return Optional.of(schema);
        } catch (IOException e) {
            log.error("Failed to read schema from classpath: {}", resourcePath, e);
            return Optional.empty();
        }
    }
}
