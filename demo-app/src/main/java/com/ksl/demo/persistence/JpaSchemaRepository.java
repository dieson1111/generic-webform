package com.ksl.demo.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genericform.core.FormComponent;
import com.genericform.core.FormSchema;
import com.genericform.core.SchemaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA-backed implementation of the library's {@link SchemaRepository} SPI.
 * <p>
 * Persists {@link FormSchema} definitions to the {@code form_schemas} table,
 * serialising the component tree as JSON.
 * </p>
 */
@Slf4j
@Repository
public class JpaSchemaRepository implements SchemaRepository {

    private final FormSchemaSpringRepo springRepo;
    private final ObjectMapper objectMapper;

    public JpaSchemaRepository(FormSchemaSpringRepo springRepo, ObjectMapper objectMapper) {
        this.springRepo = springRepo;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(FormSchema schema) {
        FormSchemaEntity entity = FormSchemaEntity.builder()
                .formId(schema.getFormId())
                .formName(schema.getFormName())
                .version(schema.getVersion())
                .componentsJson(toJson(schema.getComponents()))
                .build();
        springRepo.save(entity);
        log.debug("Saved schema to DB: formId={}", schema.getFormId());
    }

    @Override
    public Optional<FormSchema> findById(String formId) {
        return springRepo.findById(formId).map(this::toFormSchema);
    }

    @Override
    public List<FormSchema> findAll() {
        return springRepo.findAll().stream()
                .map(this::toFormSchema)
                .toList();
    }

    @Override
    public void deleteById(String formId) {
        springRepo.deleteById(formId);
        log.debug("Deleted schema from DB: formId={}", formId);
    }

    @Override
    public boolean existsById(String formId) {
        return springRepo.existsById(formId);
    }

    // ─────────────────────────── helpers ───────────────────────────

    private FormSchema toFormSchema(FormSchemaEntity entity) {
        return FormSchema.builder()
                .formId(entity.getFormId())
                .formName(entity.getFormName())
                .version(entity.getVersion())
                .components(fromJson(entity.getComponentsJson()))
                .build();
    }

    private String toJson(List<FormComponent> components) {
        try {
            return objectMapper.writeValueAsString(components);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialise form components to JSON", e);
        }
    }

    private List<FormComponent> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialise form components from JSON", e);
        }
    }
}
