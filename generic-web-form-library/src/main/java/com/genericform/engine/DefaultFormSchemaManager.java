package com.genericform.engine;

import com.genericform.core.FormComponent;
import com.genericform.core.FormColumn;
import com.genericform.core.FormSchema;
import com.genericform.core.FormSchemaManager;
import com.genericform.core.SchemaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Default implementation of {@link FormSchemaManager}.
 * <p>
 * Validates schema structure before persisting and evicts the
 * {@link FormRegistry} cache on update/delete to ensure consistency.
 * </p>
 */
@Slf4j
@Service
public class DefaultFormSchemaManager implements FormSchemaManager {

    private final SchemaRepository schemaRepository;
    private final FormRegistry formRegistry;

    public DefaultFormSchemaManager(SchemaRepository schemaRepository, FormRegistry formRegistry) {
        this.schemaRepository = schemaRepository;
        this.formRegistry = formRegistry;
    }

    @Override
    public FormSchema createSchema(FormSchema schema) {
        validateSchema(schema);

        if (schemaRepository.existsById(schema.getFormId())) {
            throw new IllegalArgumentException(
                    "Schema already exists for formId: " + schema.getFormId()
                            + ". Use updateSchema() instead.");
        }

        schemaRepository.save(schema);
        log.info("Created form schema: formId={}, version={}", schema.getFormId(), schema.getVersion());
        return schema;
    }

    @Override
    public FormSchema updateSchema(FormSchema schema) {
        validateSchema(schema);

        if (!schemaRepository.existsById(schema.getFormId())) {
            throw new IllegalArgumentException(
                    "Schema not found for formId: " + schema.getFormId()
                            + ". Use createSchema() to create a new one.");
        }

        schemaRepository.save(schema);
        formRegistry.evict(schema.getFormId());
        log.info("Updated form schema: formId={}, version={} (cache evicted)",
                schema.getFormId(), schema.getVersion());
        return schema;
    }

    @Override
    public Optional<FormSchema> getSchema(String formId) {
        return schemaRepository.findById(formId);
    }

    @Override
    public List<FormSchema> listSchemas() {
        return schemaRepository.findAll();
    }

    @Override
    public void deleteSchema(String formId) {
        if (!schemaRepository.existsById(formId)) {
            throw new IllegalArgumentException("Schema not found for formId: " + formId);
        }

        schemaRepository.deleteById(formId);
        formRegistry.evict(formId);
        log.info("Deleted form schema: formId={} (cache evicted)", formId);
    }

    // ─────────────────────────── Validation ───────────────────────────

    /**
     * Validate the structure of a schema before persisting.
     */
    private void validateSchema(FormSchema schema) {
        if (schema == null) {
            throw new IllegalArgumentException("Schema must not be null");
        }
        if (schema.getFormId() == null || schema.getFormId().isBlank()) {
            throw new IllegalArgumentException("Schema formId must not be blank");
        }
        if (schema.getComponents() == null || schema.getComponents().isEmpty()) {
            throw new IllegalArgumentException("Schema must have at least one component");
        }

        validateComponents(schema.getComponents());
    }

    /**
     * Recursively validate component definitions.
     * <p>
     * For Form.io schemas, we only enforce that each component has a
     * non-blank {@code type}. Input components should have a non-blank {@code key}.
     * </p>
     */
    private void validateComponents(List<FormComponent> components) {
        if (components == null) {
            return;
        }
        for (FormComponent component : components) {
            if (component.getType() == null || component.getType().isBlank()) {
                throw new IllegalArgumentException(
                        "Component type must not be blank (key: " + component.getKey() + ")");
            }

            // Input components must have a key
            if (component.isInput()
                    && !"button".equalsIgnoreCase(component.getType())
                    && (component.getKey() == null || component.getKey().isBlank())) {
                throw new IllegalArgumentException(
                        "Input component must have a non-blank key (type: " + component.getType() + ")");
            }

            // Recurse into children
            validateComponents(component.getComponents());

            // Recurse into columns
            if (component.getColumns() != null) {
                for (FormColumn column : component.getColumns()) {
                    validateComponents(column.getComponents());
                }
            }
        }
    }
}
