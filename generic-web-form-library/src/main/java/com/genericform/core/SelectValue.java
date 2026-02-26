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
 * Represents a single option in a Form.io select/radio component.
 * <p>
 * Each option has a human-readable {@code label} and a machine-readable
 * {@code value} that appears in the submitted data. Extra properties
 * are preserved via {@link #additionalProperties}.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SelectValue {

    /** Display label shown to the user (e.g. "Chief Executive"). */
    private String label;

    /** Machine value submitted in the form data (e.g. "chiefExecutive"). */
    private String value;

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
