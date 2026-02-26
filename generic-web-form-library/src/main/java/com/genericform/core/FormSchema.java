package com.genericform.core;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a Form.io form schema definition.
 * <p>
 * The root JSON object contains metadata ({@code form_id}, {@code form_name},
 * {@code form_version}) and a recursive tree of {@link FormComponent}
 * definitions under {@code components}. Extra top-level properties are
 * preserved via {@link #additionalProperties}.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormSchema {

    /** Unique identifier for this form (e.g. "64a9c8e5b1c2f"). */
    @JsonProperty("form_id")
    private String formId;

    /** Human-readable form name (e.g. "Sample Form"). */
    @JsonProperty("form_name")
    private String formName;

    /** Schema version string (e.g. "1.0", "v2"). */
    @JsonProperty("form_version")
    private String version;

    /** Root-level component tree defining the form structure. */
    private List<FormComponent> components;

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
