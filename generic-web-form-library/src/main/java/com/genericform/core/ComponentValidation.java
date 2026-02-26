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
     * Custom JavaScript validation expression (stored, not executed server-side).
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
