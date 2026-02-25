package com.genericform.autoconfigure;

import com.genericform.core.FormSchema;
import com.genericform.core.SchemaProvider;
import com.genericform.core.SchemaRepository;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * Implementation of {@link SchemaProvider} that delegates to a
 * {@link SchemaRepository}.
 * <p>
 * This allows the {@link com.genericform.engine.FormRegistry} to load schemas
 * that were
 * created or updated via the {@link com.genericform.core.FormSchemaManager}.
 * </p>
 */
@Slf4j
public class RepositorySchemaProvider implements SchemaProvider {

    private final SchemaRepository schemaRepository;

    public RepositorySchemaProvider(SchemaRepository schemaRepository) {
        this.schemaRepository = schemaRepository;
    }

    @Override
    public Optional<FormSchema> load(String formId) {
        log.debug("Loading schema from repository: formId={}", formId);
        return schemaRepository.findById(formId);
    }
}
