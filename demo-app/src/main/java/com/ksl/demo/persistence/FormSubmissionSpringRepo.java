package com.ksl.demo.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data repository for {@link FormSubmissionEntity}.
 */
public interface FormSubmissionSpringRepo extends JpaRepository<FormSubmissionEntity, UUID> {
    List<FormSubmissionEntity> findByFormIdOrderBySubmittedAtDesc(String formId);
}
