package com.ksl.demo.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genericform.core.FormEngine;
import com.genericform.core.FormSchema;
import com.genericform.core.FormSchemaManager;
import com.genericform.core.SubmissionResult;
import com.ksl.demo.persistence.FormSubmissionEntity;
import com.ksl.demo.persistence.FormSubmissionSpringRepo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller demonstrating usage of the generic-web-form-library.
 * <p>
 * This controller is the host application's responsibility — the library
 * is headless and does not provide its own controllers.
 * </p>
 */
@RestController
@RequestMapping("/api/forms")
public class FormController {

    private final FormEngine formEngine;
    private final FormSchemaManager formSchemaManager;
    private final FormSubmissionSpringRepo submissionRepo;
    private final ObjectMapper objectMapper;

    public FormController(FormEngine formEngine,
            FormSchemaManager formSchemaManager,
            FormSubmissionSpringRepo submissionRepo,
            ObjectMapper objectMapper) {
        this.formEngine = formEngine;
        this.formSchemaManager = formSchemaManager;
        this.submissionRepo = submissionRepo;
        this.objectMapper = objectMapper;
    }

    // ─────────────────────────── Schema CRUD ───────────────────────────

    /**
     * List all registered form schemas.
     */
    @GetMapping
    public ResponseEntity<List<FormSchema>> listSchemas() {
        return ResponseEntity.ok(formSchemaManager.listSchemas());
    }

    /**
     * Retrieve a specific form schema by its formId.
     */
    @GetMapping("/{formId}")
    public ResponseEntity<FormSchema> getSchema(@PathVariable String formId) {
        return formSchemaManager.getSchema(formId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a new form schema.
     */
    @PostMapping
    public ResponseEntity<FormSchema> createSchema(@RequestBody FormSchema schema) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(formSchemaManager.createSchema(schema));
    }

    /**
     * Update an existing form schema.
     */
    @PutMapping("/{formId}")
    public ResponseEntity<FormSchema> updateSchema(@PathVariable String formId,
            @RequestBody FormSchema schema) {
        schema.setFormId(formId);
        return ResponseEntity.ok(formSchemaManager.updateSchema(schema));
    }

    /**
     * Delete a form schema.
     */
    @DeleteMapping("/{formId}")
    public ResponseEntity<Void> deleteSchema(@PathVariable String formId) {
        formSchemaManager.deleteSchema(formId);
        return ResponseEntity.noContent().build();
    }

    // ─────────────────────────── Form Submission ───────────────────────

    /**
     * Submit data against a form schema.
     * The library validates the data and persists it if valid.
     */
    @PostMapping("/{formId}/submit")
    public ResponseEntity<SubmissionResult> submitForm(@PathVariable String formId,
            @RequestBody Map<String, Object> data) {
        SubmissionResult result = formEngine.process(formId, data);
        if (result.isValid()) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.badRequest().body(result);
    }

    /**
     * Retrieve all submissions for a specific form.
     */
    @GetMapping("/{formId}/submissions")
    public ResponseEntity<List<Map<String, Object>>> getSubmissions(@PathVariable String formId) {
        if (formSchemaManager.getSchema(formId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<FormSubmissionEntity> entities = submissionRepo.findByFormIdOrderBySubmittedAtDesc(formId);
        List<Map<String, Object>> submissions = entities.stream()
                .map(this::toDataMap)
                .collect(Collectors.toList());

        return ResponseEntity.ok(submissions);
    }

    private Map<String, Object> toDataMap(FormSubmissionEntity entity) {
        try {
            return objectMapper.readValue(entity.getDataJson(), new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialise submission data", e);
        }
    }
}
