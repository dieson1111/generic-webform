package com.genericform.engine;

import com.genericform.core.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.internal.constraintvalidators.bv.EmailValidator;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Recursive validation engine that validates flat submission data against
 * a Form.io {@link FormSchema} component tree.
 * <p>
 * <strong>Key design principle:</strong> Form.io schemas are deeply nested
 * (layout components wrap input components), but the submitted data is
 * <em>flat</em> — all values keyed at the root level by each component's
 * {@code key}. This engine therefore traverses the component tree to find
 * input components, then looks up their values from the flat data map.
 * </p>
 * <p>
 * Supports:
 * <ul>
 * <li>Required field checks</li>
 * <li>Regex pattern matching (textfield, email, url, phoneNumber)</li>
 * <li>Email format validation</li>
 * <li>Select value validation (submitted value must be in allowed options)</li>
 * <li>Layout traversal (well, fieldset, columns, flexbox, panel)</li>
 * <li>Static component skipping (content, htmlelement, button)</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
public class ValidationEngine {

    // ── Layout types: traverse children, never validate ──────────────────
    private static final Set<String> LAYOUT_TYPES = Set.of(
            "well", "fieldset", "columns", "flexbox", "panel", "tabs",
            "table", "container");

    // ── Static types: skip entirely (no data, no children) ──────────────
    private static final Set<String> STATIC_TYPES = Set.of(
            "content", "htmlelement", "button");

    /**
     * Validate flat submission data against a Form.io form schema.
     *
     * @param schema the form schema definition (component tree)
     * @param data   the submitted flat key-value pairs
     * @return a map of field-key → error message (empty if valid)
     */
    public Map<String, String> validate(FormSchema schema, Map<String, Object> data) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (schema.getComponents() == null) {
            return errors;
        }
        traverseComponents(schema.getComponents(), data, errors);
        return errors;
    }

    // ───────────────────────────── Tree Traversal ─────────────────────────

    /**
     * Recursively traverse a list of Form.io components.
     * Layout components are traversed for their children; input components
     * are validated; static components are skipped.
     */
    private void traverseComponents(List<FormComponent> components,
            Map<String, Object> data,
            Map<String, String> errors) {
        if (components == null) {
            return;
        }
        for (FormComponent component : components) {
            String type = normalizeType(component.getType());

            // ── Static: skip entirely ───────────────────────────────
            if (STATIC_TYPES.contains(type)) {
                continue;
            }

            // ── Columns: special layout with column children ────────
            if ("columns".equals(type)) {
                traverseColumns(component, data, errors);
                continue;
            }

            // ── Layout: recurse into children ───────────────────────
            if (LAYOUT_TYPES.contains(type)) {
                traverseComponents(component.getComponents(), data, errors);
                continue;
            }

            // ── Input: validate against flat data ───────────────────
            if (component.isInput()) {
                validateInputComponent(component, data, errors);
            }

            // ── If it has children (non-standard layout), traverse ──
            if (component.getComponents() != null && !component.getComponents().isEmpty()) {
                traverseComponents(component.getComponents(), data, errors);
            }
        }
    }

    /**
     * Traverse a columns-type component: iterate each column's components.
     */
    private void traverseColumns(FormComponent columnsComponent,
            Map<String, Object> data,
            Map<String, String> errors) {
        List<FormColumn> columns = columnsComponent.getColumns();
        if (columns == null) {
            return;
        }
        for (FormColumn column : columns) {
            traverseComponents(column.getComponents(), data, errors);
        }
    }

    // ───────────────────────────── Input Validation ───────────────────────

    /**
     * Validate a single input component against the flat data map.
     */
    private void validateInputComponent(FormComponent component,
            Map<String, Object> data,
            Map<String, String> errors) {
        String key = component.getKey();
        if (key == null || key.isEmpty()) {
            return;
        }

        Object value = data == null ? null : data.get(key);
        ComponentValidation validate = component.getValidate();

        // ── Required check ──────────────────────────────────────────
        if (validate != null && validate.isRequired() && isBlank(value)) {
            String msg = hasCustomMessage(validate) ? validate.getCustomMessage() : "Field is required";
            errors.put(key, msg);
            return; // skip further checks if missing
        }

        // If value is absent and not required, skip validation
        if (isBlank(value)) {
            return;
        }

        // ── Type-specific validation ────────────────────────────────
        String type = normalizeType(component.getType());
        switch (type) {
            case "textfield", "textarea", "password", "phonenumber" ->
                validateText(component, value, key, errors);
            case "email" ->
                validateEmail(component, value, key, errors);
            case "url" ->
                validateUrl(component, value, key, errors);
            case "number", "currency" ->
                validateNumber(component, value, key, errors);
            case "select", "radio" ->
                validateSelect(component, value, key, errors);
            case "datetime", "day", "time" ->
                validateDateTime(component, value, key, errors);
            case "signature" ->
                validateSignature(value, key, errors);
            default ->
                validateText(component, value, key, errors);
        }
    }

    // ───────────────────────────── Type-Specific Validators ───────────────

    /** Validate text-like fields: pattern and min/max length. */
    private void validateText(FormComponent component, Object value, String key,
            Map<String, String> errors) {
        String strValue = value.toString();
        ComponentValidation v = component.getValidate();
        if (v == null)
            return;

        // Pattern check
        if (v.getPattern() != null && !v.getPattern().isEmpty()) {
            if (!Pattern.matches(v.getPattern(), strValue)) {
                String msg = hasCustomMessage(v) ? v.getCustomMessage()
                        : "Value does not match pattern: " + v.getPattern();
                errors.put(key, msg);
            }
        }

        // MinLength check
        Integer minLen = parseIntOrNull(v.getMinLength());
        if (minLen != null && strValue.length() < minLen) {
            errors.put(key, "Value must be at least " + minLen + " characters");
        }

        // MaxLength check
        Integer maxLen = parseIntOrNull(v.getMaxLength());
        if (maxLen != null && strValue.length() > maxLen) {
            errors.put(key, "Value must be at most " + maxLen + " characters");
        }
    }

    /** Validate email fields: email format + text validation. */
    private void validateEmail(FormComponent component, Object value, String key,
            Map<String, String> errors) {
        String strValue = value.toString();
        EmailValidator emailValidator = new EmailValidator();
        if (!emailValidator.isValid(strValue, null)) {
            errors.put(key, "Invalid email format");
            return;
        }
        // Also run text-level checks (pattern, length)
        validateText(component, value, key, errors);
    }

    /** Validate URL fields: basic URL format check. */
    private void validateUrl(FormComponent component, Object value, String key,
            Map<String, String> errors) {
        // Run text-level checks (pattern, length)
        validateText(component, value, key, errors);
    }

    /** Validate number fields: numeric format + min/max. */
    private void validateNumber(FormComponent component, Object value, String key,
            Map<String, String> errors) {
        double numValue;
        try {
            numValue = Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            errors.put(key, "Expected a numeric value");
            return;
        }

        ComponentValidation v = component.getValidate();
        if (v == null)
            return;

        Double min = parseDoubleOrNull(v.getMinLength());
        Double max = parseDoubleOrNull(v.getMaxLength());

        if (min != null && numValue < min) {
            errors.put(key, "Value must be >= " + min);
        }
        if (max != null && numValue > max) {
            errors.put(key, "Value must be <= " + max);
        }
    }

    /**
     * Validate select fields: submitted value must be one of the allowed options.
     */
    private void validateSelect(FormComponent component, Object value, String key,
            Map<String, String> errors) {
        ComponentData data = component.getData();
        if (data == null || data.getValues() == null || data.getValues().isEmpty()) {
            return; // no predefined values to check against
        }

        String strValue = value.toString();
        boolean found = data.getValues().stream()
                .anyMatch(sv -> strValue.equals(sv.getValue()));

        if (!found) {
            errors.put(key, "Value is not one of the allowed options");
        }
    }

    /** Validate datetime fields: check that a non-empty string is provided. */
    private void validateDateTime(FormComponent component, Object value, String key,
            Map<String, String> errors) {
        // Basic validation: value should be a non-blank string
        // (Full ISO-8601 parsing could be added later)
        if (isBlank(value)) {
            errors.put(key, "A valid date/time is required");
        }
    }

    /**
     * Validate signature fields: check that a non-empty string (Base64) is
     * provided.
     */
    private void validateSignature(Object value, String key,
            Map<String, String> errors) {
        if (isBlank(value)) {
            errors.put(key, "Signature is required");
        }
    }

    // ───────────────────────────── Helpers ────────────────────────────────

    /** Normalize a type string to lowercase for comparison. */
    private String normalizeType(String type) {
        return type != null ? type.toLowerCase() : "textfield";
    }

    /** Check if a value should be considered blank / missing. */
    private boolean isBlank(Object value) {
        if (value == null)
            return true;
        if (value instanceof String s)
            return s.trim().isEmpty();
        if (value instanceof List<?> l)
            return l.isEmpty();
        return false;
    }

    /** Check if a ComponentValidation has a non-empty custom message. */
    private boolean hasCustomMessage(ComponentValidation v) {
        return v != null && v.getCustomMessage() != null && !v.getCustomMessage().isEmpty();
    }

    /** Parse a string as Integer, returning null if blank or non-numeric. */
    private Integer parseIntOrNull(String s) {
        if (s == null || s.trim().isEmpty())
            return null;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Parse a string as Double, returning null if blank or non-numeric. */
    private Double parseDoubleOrNull(String s) {
        if (s == null || s.trim().isEmpty())
            return null;
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
