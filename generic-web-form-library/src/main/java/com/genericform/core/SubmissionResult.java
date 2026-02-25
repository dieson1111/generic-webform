package com.genericform.core;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.Collections;
import java.util.Map;

/**
 * Immutable result returned after processing a form submission.
 */
@Getter
@ToString
@AllArgsConstructor
public class SubmissionResult {

    /** {@code true} if the submission passed all validation rules. */
    private final boolean valid;

    /**
     * Validation errors keyed by field path (e.g. {@code "addresses[0].city"}).
     * Empty when the submission is valid.
     */
    private final Map<String, String> errors;

    /** Factory method for a successful result. */
    public static SubmissionResult success() {
        return new SubmissionResult(true, Collections.emptyMap());
    }

    /** Factory method for a failed result. */
    public static SubmissionResult failure(Map<String, String> errors) {
        return new SubmissionResult(false, Collections.unmodifiableMap(errors));
    }
}
