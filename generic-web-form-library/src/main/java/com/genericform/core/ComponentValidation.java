package com.genericform.core;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps the nested {@code validate} object from a Form.io component.
 * <p>
 * Fields actively used by the validation engine are declared as typed
 * Java fields. All other validation properties (e.g. {@code customPrivate},
 * {@code strictDateValidation}, {@code json}) are captured in
 * {@link #additionalProperties} for lossless round-trip serialization.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentValidation {

    /** Whether this field is mandatory. */
    private boolean required;

    /** Regex pattern the value must match. */
    private String pattern;

    /** Minimum string length. */
    private String minLength;

    /** Maximum string length. */
    private String maxLength;

    /** Custom error message to display when validation fails. */
    private String customMessage;

    /**
     * Custom JavaScript validation expression evaluated server-side via GraalVM.
     * <p>
     * The script must set a variable {@code valid} to either {@code true}
     * (passed) or an error-message string (failed). Available bindings:
     * <ul>
     * <li>{@code input} — the value of the current field</li>
     * <li>{@code data} — the full flat submission data (enables cross-field
     * checks)</li>
     * <li>{@code row} — same as data for flat forms; scoped to the row for
     * edit-grids</li>
     * <li>{@code component} — component metadata (key, type, label)</li>
     * </ul>
     * </p>
     */
    private String custom;

    /** For select: only allow values from the predefined list. */
    private boolean onlyAvailableItems;

    // ─────────────────────────── Catch-All ────────────────────────────────

    @Builder.Default
    private Map<String, Object> additionalProperties = new LinkedHashMap<>();

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        if (additionalProperties == null) {
            additionalProperties = new LinkedHashMap<>();
        }
        additionalProperties.put(name, value);
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }
}
