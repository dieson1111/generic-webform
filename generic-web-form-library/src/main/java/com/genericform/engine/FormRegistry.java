package com.genericform.engine;

import com.genericform.core.FormSchema;
import com.genericform.core.SchemaProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry that caches {@link FormSchema} instances loaded via
 * {@link SchemaProvider}.
 * <p>
 * Schemas are cached in a {@link ConcurrentHashMap} keyed by {@code formId}.
 * Supports versioned lookup — e.g. a schema file named {@code survey_v1.json}
 * would be loaded as {@code formId = "survey_v1"}.
 * </p>
 */
@Slf4j
@Component
public class FormRegistry {

    private final SchemaProvider schemaProvider;
    private final ConcurrentHashMap<String, FormSchema> cache = new ConcurrentHashMap<>();

    public FormRegistry(SchemaProvider schemaProvider) {
        this.schemaProvider = schemaProvider;
    }

    /**
     * Retrieve a schema by its {@code formId}, loading and caching on first access.
     *
     * @param formId the form identifier (e.g. "survey", "survey_v1")
     * @return the cached schema, or empty if the provider cannot find it
     */
    public Optional<FormSchema> get(String formId) {
        FormSchema cached = cache.get(formId);
        if (cached != null) {
            return Optional.of(cached);
        }

        Optional<FormSchema> loaded = schemaProvider.load(formId);
        loaded.ifPresent(schema -> {
            cache.put(formId, schema);
            log.info("Cached form schema: formId={}, version={}", formId, schema.getVersion());
        });
        return loaded;
    }

    /**
     * Evict a specific schema from the cache (useful for hot-reload scenarios).
     *
     * @param formId the form identifier to evict
     */
    public void evict(String formId) {
        cache.remove(formId);
        log.info("Evicted form schema from cache: formId={}", formId);
    }

    /**
     * Clear the entire schema cache.
     */
    public void clearCache() {
        cache.clear();
        log.info("Cleared all cached form schemas");
    }
}
