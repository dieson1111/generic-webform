package com.genericform.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genericform.core.FormEngine;
import com.genericform.core.FormRepository;
import com.genericform.core.FormSchemaManager;
import com.genericform.core.SchemaProvider;
import com.genericform.core.SchemaRepository;
import com.genericform.engine.DefaultFormEngine;
import com.genericform.engine.DefaultFormSchemaManager;
import com.genericform.engine.FormRegistry;
import com.genericform.engine.ValidationEngine;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Spring Boot auto-configuration for the Generic Web Form Engine.
 * <p>
 * Provides sensible defaults for all required beans. Every bean uses
 * {@link ConditionalOnMissingBean} so the host application can override
 * any component by defining its own bean of the same type.
 * </p>
 */
@AutoConfiguration
@EnableConfigurationProperties(GenericFormProperties.class)
public class GenericFormAutoConfiguration {

    // ───────────────────────────── Schema Provider ─────────────────────────────

    /**
     * Default {@link SchemaProvider} bean.
     * <p>
     * Selects the implementation based on {@code genericform.schema.source}:
     * <ul>
     * <li>{@code CLASSPATH} (default) → {@link ClasspathSchemaProvider}</li>
     * <li>{@code FILESYSTEM} → {@link FilesystemSchemaProvider}</li>
     * </ul>
     * If the host app registers its own {@code SchemaProvider} bean (e.g.
     * database-backed), this default backs off automatically.
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean(SchemaProvider.class)
    public SchemaProvider schemaProvider(ObjectMapper objectMapper,
            GenericFormProperties properties,
            SchemaRepository schemaRepository) {
        return switch (properties.getSource()) {
            case FILESYSTEM -> {
                if (properties.getFilesystemPath() == null || properties.getFilesystemPath().isBlank()) {
                    throw new IllegalStateException(
                            "genericform.schema.filesystem-path must be set when source=filesystem");
                }
                yield new FilesystemSchemaProvider(objectMapper, properties.getFilesystemPath());
            }
            case REPOSITORY -> new RepositorySchemaProvider(schemaRepository);
            case CLASSPATH -> new ClasspathSchemaProvider(objectMapper, properties.getClasspathPrefix());
        };
    }

    // ───────────────────────────── Repository ─────────────────────────────────

    @Bean
    @ConditionalOnMissingBean(FormRepository.class)
    public FormRepository formRepository() {
        return new NoOpFormRepository();
    }

    // ───────────────────────────── Jackson ObjectMapper ────────────────────────

    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    // ───────────────────────────── Engine Components ──────────────────────────

    @Bean
    @ConditionalOnMissingBean(FormRegistry.class)
    public FormRegistry formRegistry(SchemaProvider schemaProvider) {
        return new FormRegistry(schemaProvider);
    }

    @Bean
    @ConditionalOnMissingBean(ValidationEngine.class)
    public ValidationEngine validationEngine() {
        return new ValidationEngine();
    }

    @Bean
    @ConditionalOnMissingBean(FormEngine.class)
    public FormEngine formEngine(FormRegistry formRegistry,
            ValidationEngine validationEngine,
            FormRepository formRepository) {
        return new DefaultFormEngine(formRegistry, validationEngine, formRepository);
    }

    // ───────────────────────────── Schema Management ──────────────────────────

    @Bean
    @ConditionalOnMissingBean(SchemaRepository.class)
    public SchemaRepository schemaRepository() {
        return new InMemorySchemaRepository();
    }

    @Bean
    @ConditionalOnMissingBean(FormSchemaManager.class)
    public FormSchemaManager formSchemaManager(SchemaRepository schemaRepository,
            FormRegistry formRegistry) {
        return new DefaultFormSchemaManager(schemaRepository, formRegistry);
    }
}
