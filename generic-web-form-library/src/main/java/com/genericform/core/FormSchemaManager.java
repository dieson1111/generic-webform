package com.genericform.core;

import java.util.List;
import java.util.Optional;

/**
 * Public service interface for managing form schemas (CRUD).
 * <p>
 * This is the second entry point of the library alongside {@link FormEngine}.
 * The host application injects this bean to allow admins or backend processes
 * to create, update, retrieve, and delete form schemas at runtime.
 * </p>
 */
public interface FormSchemaManager {

    /**
     * Create a new form schema.
     *
     * @param schema the schema to create (must have a non-blank {@code formId})
     * @return the saved schema
     * @throws IllegalArgumentException if the schema is invalid or already exists
     */
    FormSchema createSchema(FormSchema schema);

    /**
     * Update an existing form schema.
     *
     * @param schema the schema to update (must have a non-blank {@code formId})
     * @return the updated schema
     * @throws IllegalArgumentException if the schema is invalid or does not exist
     */
    FormSchema updateSchema(FormSchema schema);

    /**
     * Retrieve a form schema by its identifier.
     *
     * @param formId the unique form identifier
     * @return the schema, or empty if not found
     */
    Optional<FormSchema> getSchema(String formId);

    /**
     * List all available form schemas.
     *
     * @return list of all schemas
     */
    List<FormSchema> listSchemas();

    /**
     * Delete a form schema by its identifier.
     *
     * @param formId the unique form identifier
     * @throws IllegalArgumentException if the schema does not exist
     */
    void deleteSchema(String formId);
}
