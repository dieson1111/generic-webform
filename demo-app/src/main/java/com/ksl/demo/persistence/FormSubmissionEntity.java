package com.ksl.demo.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for persisting validated form submissions.
 */
@Entity
@Table(name = "form_submissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormSubmissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "form_id", nullable = false)
    private String formId;

    /**
     * The submitted data stored as a JSON string.
     */
    @Column(name = "data_json", columnDefinition = "TEXT")
    private String dataJson;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @PrePersist
    public void prePersist() {
        if (submittedAt == null) {
            submittedAt = Instant.now();
        }
    }
}
