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
 * Represents a single component node in a Form.io component tree.
 * <p>
 * A component can be either a <strong>layout</strong> container
 * (e.g. {@code well}, {@code fieldset}, {@code columns}, {@code flexbox})
 * or an <strong>input</strong> component (e.g. {@code textfield},
 * {@code email},
 * {@code select}, {@code signature}, {@code datetime}).
 * </p>
 * <p>
 * Layout components have {@code input = false} and contain child components
 * via their {@code components} list (or {@code columns} list for column
 * layouts).
 * Input components have {@code input = true} and carry a {@code key} that
 * maps to the flat submission data.
 * </p>
 * <p>
 * Fields actively used by the backend validation engine are declared as
 * typed Java fields. All other Form.io properties (UI hints, conditional
 * logic, overlays, etc.) are captured in {@link #additionalProperties}
 * via {@link JsonAnySetter} and re-emitted on serialization via
 * {@link JsonAnyGetter}, ensuring lossless round-trip fidelity.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormComponent {

    // ─────────────────────────── Identity ──────────────────────────────────

    /** Data binding key — maps to the submission data field name. */
    private String key;

    /**
     * Component type (Form.io type string).
     * <ul>
     * <li>Layout: {@code well}, {@code fieldset}, {@code columns},
     * {@code flexbox}, {@code panel}, {@code tabs}</li>
     * <li>Input: {@code textfield}, {@code email}, {@code url},
     * {@code phoneNumber}, {@code number}, {@code select},
     * {@code datetime}, {@code signature}, {@code textarea},
     * {@code password}, {@code checkbox}, {@code radio}</li>
     * <li>Static: {@code content}, {@code htmlelement}, {@code button}</li>
     * </ul>
     */
    private String type;

    /** Display label shown to the user. */
    private String label;

    // ─────────────────────────── Flags ─────────────────────────────────────

    /**
     * {@code true} if this component is data-bearing (input component).
     * {@code false} for layout/static components.
     */
    private boolean input;

    /** Whether this component is hidden from view. */
    private boolean hidden;

    /** Whether this component is disabled / read-only. */
    private boolean disabled;

    /** Whether the value should be persisted on submission. */
    private boolean persistent;

    /** Whether multiple values are allowed for this component. */
    private boolean multiple;

    // ─────────────────────────── Validation ────────────────────────────────

    /** Nested validation rules from the Form.io {@code validate} object. */
    private ComponentValidation validate;

    /** Input mask pattern (e.g. {@code "9999-9999"} for phone numbers). */
    private String inputMask;

    // ─────────────────────────── Children (recursive) ─────────────────────

    /**
     * Child components for layout containers ({@code well}, {@code fieldset},
     * {@code flexbox}, {@code panel}, etc.).
     */
    private List<FormComponent> components;

    /**
     * Column definitions for {@code columns}-type layouts.
     * Each {@link FormColumn} contains its own list of child components.
     */
    private List<FormColumn> columns;

    // ─────────────────────────── Select Data ──────────────────────────────

    /**
     * Data source configuration for {@code select} / {@code radio} components.
     * Contains the list of allowed {@link SelectValue} options.
     */
    private ComponentData data;

    /**
     * Data source type for select components (e.g. {@code "values"},
     * {@code "url"}).
     */
    private String dataSrc;

    // ─────────────────────────── Catch-All (extra Form.io properties) ─────

    /**
     * Stores all Form.io component properties that are not explicitly
     * declared as typed Java fields above (e.g. {@code tooltip},
     * {@code customClass}, {@code conditional}, {@code overlay},
     * {@code datePicker}, {@code widget}, etc.).
     * <p>
     * This ensures lossless round-trip serialization: every property
     * present in the original JSON is preserved when the schema is
     * stored and later returned to the frontend.
     * </p>
     */
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
