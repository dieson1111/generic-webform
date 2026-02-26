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
 * Holds the data source configuration for a Form.io select/radio component.
 * <p>
 * When {@code dataSrc} is {@code "values"}, the {@code values} list contains
 * the allowed {@link SelectValue} options. Other data source types
 * ({@code url}, {@code resource}, {@code custom}) are stored but not
 * validated server-side. Extra properties are preserved via
 * {@link #additionalProperties}.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentData {

    /** The list of allowed options (used when dataSrc = "values"). */
    private List<SelectValue> values;

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
