package com.genericform.core;

import java.util.Map;

/**
 * Main entry-point interface for the Generic Web Form Engine.
 * <p>
 * The host application injects this bean and delegates form processing to it.
 * This library is <strong>headless</strong> — the host application owns the
 * API / controller layer.
 * </p>
 */
public interface FormEngine {

    /**
     * Process a form submission: load the schema, validate input, and
     * persist if valid.
     *
     * @param formId the unique form identifier (e.g. "survey")
     * @param data   the submitted key-value data
     * @return a {@link SubmissionResult} indicating success or validation errors
     */
    SubmissionResult process(String formId, Map<String, Object> data);
}
