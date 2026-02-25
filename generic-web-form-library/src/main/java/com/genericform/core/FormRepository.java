package com.genericform.core;

import java.util.Map;

/**
 * SPI interface for persisting validated form submissions.
 * <p>
 * The host application provides its own implementation (e.g. JPA, MongoDB)
 * and registers it as a Spring bean. If no implementation is provided,
 * the library falls back to a no-op logger.
 * </p>
 */
public interface FormRepository {

    /**
     * Persist a validated form submission.
     *
     * @param formId the form identifier
     * @param data   the submitted key-value data
     */
    void save(String formId, Map<String, Object> data);
}
