package com.ksl.demo.persistence;

import jakarta.persistence.*;
import lombok.*;

/**
 * JPA entity for persisting form schema definitions.
 */
@Entity
@Table(name = "form_schemas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormSchemaEntity {

    @Id
    @Column(name = "form_id", nullable = false, unique = true)
    private String formId;

    @Column(name = "form_name")
    private String formName;

    @Column(name = "version")
    private String version;

    /**
     * The component tree definitions stored as a JSON string.
     */
    @Column(name = "components_json", columnDefinition = "TEXT")
    private String componentsJson;
}
