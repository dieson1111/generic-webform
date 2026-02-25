package com.genericform.core;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a single column within a Form.io {@code columns} layout component.
 * <p>
 * Each column contains its own list of child {@link FormComponent}s and
 * a {@code width} (Bootstrap grid units, typically 1–12).
 * </p>
 *
 * <pre>
 * { "components": [...], "width": 6, "offset": 0 }
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FormColumn {

    /** Child components within this column. */
    private List<FormComponent> components;

    /** Column width in Bootstrap grid units (1–12). */
    private int width;

    /** Column offset in Bootstrap grid units. */
    private int offset;
}
