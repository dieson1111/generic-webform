package com.genericform.core;

import java.util.Optional;

/**
 * SPI interface for loading {@link FormSchema} definitions.
 * <p>
 * Built-in implementations read from the classpath or filesystem.
 * The host application may provide a custom bean (e.g. database-backed)
 * to override the defaults.
 * </p>
 */
public interface SchemaProvider {

    /**
     * Load a form schema by its identifier.
     *
     * @param formId the unique form identifier (e.g. "survey")
     * @return the schema wrapped in an {@link Optional}, or empty if not found
     */
    Optional<FormSchema> load(String formId);
}
