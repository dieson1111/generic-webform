package com.genericform.core;

import java.util.List;
import java.util.Optional;

/**
 * SPI interface for persisting {@link FormSchema} definitions (CRUD).
 * <p>
 * This is separate from {@link FormRepository} which handles
 * <em>submission data</em>. This interface handles the schemas themselves.
 * </p>
 * <p>
 * The host application provides its own implementation (e.g. JPA, MongoDB)
 * and registers it as a Spring bean. If none is provided, the library
 * falls back to an in-memory implementation.
 * </p>
 */
public interface SchemaRepository {

    /**
     * Save (create or update) a form schema.
     *
     * @param schema the form schema to persist
     */
    void save(FormSchema schema);

    /**
     * Find a form schema by its identifier.
     *
     * @param formId the unique form identifier
     * @return the schema, or empty if not found
     */
    Optional<FormSchema> findById(String formId);

    /**
     * Retrieve all stored form schemas.
     *
     * @return list of all schemas (may be empty, never null)
     */
    List<FormSchema> findAll();

    /**
     * Delete a form schema by its identifier.
     *
     * @param formId the unique form identifier
     */
    void deleteById(String formId);

    /**
     * Check whether a schema exists for the given identifier.
     *
     * @param formId the unique form identifier
     * @return {@code true} if a schema exists
     */
    boolean existsById(String formId);
}
