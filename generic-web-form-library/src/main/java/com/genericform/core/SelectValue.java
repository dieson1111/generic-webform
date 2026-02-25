package com.genericform.core;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single option in a Form.io select/radio component.
 * <p>
 * Each option has a human-readable {@code label} and a machine-readable
 * {@code value} that appears in the submitted data.
 * </p>
 *
 * <pre>
 * { "label": "Chief Executive", "value": "chiefExecutive" }
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SelectValue {

    /** Display label shown to the user (e.g. "Chief Executive"). */
    private String label;

    /** Machine value submitted in the form data (e.g. "chiefExecutive"). */
    private String value;
}
