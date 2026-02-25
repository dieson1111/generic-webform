package com.genericform.autoconfigure;

import com.genericform.core.FormSchema;
import com.genericform.core.SchemaRepository;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default in-memory {@link SchemaRepository} implementation.
 * <p>
 * Stores schemas in a {@link ConcurrentHashMap} — suitable for
 * development and testing. Data is <strong>lost on restart</strong>.
 * </p>
 * <p>
 * For production, the host application should provide its own
 * {@code SchemaRepository} bean backed by a database (JPA, MongoDB, etc.).
 * </p>
 */
@Slf4j
public class InMemorySchemaRepository implements SchemaRepository {

    private final ConcurrentHashMap<String, FormSchema> store = new ConcurrentHashMap<>();

    @Override
    public void save(FormSchema schema) {
        store.put(schema.getFormId(), schema);
        log.debug("[InMemorySchemaRepository] Saved schema: formId={}", schema.getFormId());
    }

    @Override
    public Optional<FormSchema> findById(String formId) {
        return Optional.ofNullable(store.get(formId));
    }

    @Override
    public List<FormSchema> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void deleteById(String formId) {
        store.remove(formId);
        log.debug("[InMemorySchemaRepository] Deleted schema: formId={}", formId);
    }

    @Override
    public boolean existsById(String formId) {
        return store.containsKey(formId);
    }
}
