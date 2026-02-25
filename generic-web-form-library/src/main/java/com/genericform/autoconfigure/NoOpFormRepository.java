package com.genericform.autoconfigure;

import com.genericform.core.FormRepository;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * No-op {@link FormRepository} that simply logs the submitted data.
 * <p>
 * This allows the application to start without a real persistence
 * implementation. Replace by registering your own {@code FormRepository} bean.
 * </p>
 */
@Slf4j
public class NoOpFormRepository implements FormRepository {

    @Override
    public void save(String formId, Map<String, Object> data) {
        log.info("[NoOpFormRepository] Received submission for formId={}, data={}", formId, data);
        log.warn("No FormRepository implementation provided. Data was NOT persisted. "
                + "Register a FormRepository bean to enable persistence.");
    }
}
