package com.ksl.demo.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genericform.core.FormRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Map;

/**
 * JPA-backed implementation of the library's {@link FormRepository} SPI.
 * <p>
 * Persists validated form submission data to the {@code form_submissions}
 * table.
 * </p>
 */
@Slf4j
@Repository
public class JpaFormRepository implements FormRepository {

    private final FormSubmissionSpringRepo springRepo;
    private final ObjectMapper objectMapper;

    public JpaFormRepository(FormSubmissionSpringRepo springRepo, ObjectMapper objectMapper) {
        this.springRepo = springRepo;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(String formId, Map<String, Object> data) {
        FormSubmissionEntity entity = FormSubmissionEntity.builder()
                .formId(formId)
                .dataJson(toJson(data))
                .build();
        springRepo.save(entity);
        log.info("Saved submission to DB: formId={}, id={}", formId, entity.getId());
    }

    private String toJson(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialise submission data to JSON", e);
        }
    }
}
