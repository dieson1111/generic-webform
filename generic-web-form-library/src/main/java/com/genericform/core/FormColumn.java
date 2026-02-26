package com.genericform.core;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a single column within a Form.io {@code columns} layout component.
 * <p>
 * Each column contains its own list of child {@link FormComponent}s and
 * a {@code width} (Bootstrap grid units, typically 1–12). Extra properties
 * like {@code push}, {@code pull}, {@code size}, {@code currentWidth} are
 * preserved via {@link #additionalProperties}.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormColumn {

    /** Child components within this column. */
    private List<FormComponent> components;

    /** Column width in Bootstrap grid units (1–12). */
    private int width;

    /** Column offset in Bootstrap grid units. */
    private int offset;

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
