package com.genericform.engine;

import com.genericform.core.FormEngine;
import com.genericform.core.FormRepository;
import com.genericform.core.FormSchema;
import com.genericform.core.SubmissionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Default implementation of {@link FormEngine}.
 * <p>
 * Processing flow:
 * <ol>
 * <li>Load the schema via {@link FormRegistry}</li>
 * <li>Validate input via {@link ValidationEngine}</li>
 * <li>If valid, persist via {@link FormRepository}</li>
 * <li>Return {@link SubmissionResult}</li>
 * </ol>
 * </p>
 */
@Slf4j
@Service
public class DefaultFormEngine implements FormEngine {

    private final FormRegistry formRegistry;
    private final ValidationEngine validationEngine;
    private final FormRepository formRepository;

    public DefaultFormEngine(FormRegistry formRegistry,
            ValidationEngine validationEngine,
            FormRepository formRepository) {
        this.formRegistry = formRegistry;
        this.validationEngine = validationEngine;
        this.formRepository = formRepository;
    }

    @Override
    public SubmissionResult process(String formId, Map<String, Object> data) {
        log.debug("Processing form submission: formId={}", formId);

        // 1. Load schema
        FormSchema schema = formRegistry.get(formId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Form schema not found for formId: " + formId));

        // 2. Validate
        Map<String, String> errors = validationEngine.validate(schema, data);

        if (!errors.isEmpty()) {
            log.info("Validation failed for formId={}: {} error(s)", formId, errors.size());
            return SubmissionResult.failure(errors);
        }

        // 3. Persist
        formRepository.save(formId, data);
        log.info("Form submission saved: formId={}", formId);

        return SubmissionResult.success();
    }
}
