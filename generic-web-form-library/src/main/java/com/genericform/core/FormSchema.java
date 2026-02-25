package com.genericform.core;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a Form.io form schema definition.
 * <p>
 * The root JSON object contains metadata ({@code form_id}, {@code form_name},
 * {@code form_version}) and a recursive tree of {@link FormComponent}
 * definitions under {@code components}.
 * </p>
 *
 * <pre>
 * {
 *   "form_id": "64a9c8e5b1c2f",
 *   "form_name": "Sample Form",
 *   "form_version": "1.0",
 *   "components": [...]
 * }
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
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
}
