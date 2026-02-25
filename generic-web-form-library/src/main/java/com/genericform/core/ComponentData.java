package com.genericform.core;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Holds the data source configuration for a Form.io select/radio component.
 * <p>
 * When {@code dataSrc} is {@code "values"}, the {@code values} list contains
 * the allowed {@link SelectValue} options. Other data source types
 * ({@code url}, {@code resource}, {@code custom}) are stored but not
 * validated server-side.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ComponentData {

    /** The list of allowed options (used when dataSrc = "values"). */
    private List<SelectValue> values;
}
