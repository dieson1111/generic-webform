package com.genericform.core;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Maps the nested {@code validate} object from a Form.io component.
 * <p>
 * Example JSON:
 * 
 * <pre>
 * "validate": {
 *   "required": true,
 *   "pattern": "^[0-9]{5}$",
 *   "minLength": 1,
 *   "maxLength": 100,
 *   "customMessage": "Please enter a valid postal code",
 *   "custom": "",
 *   "onlyAvailableItems": false
 * }
 * </pre>
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
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
}
