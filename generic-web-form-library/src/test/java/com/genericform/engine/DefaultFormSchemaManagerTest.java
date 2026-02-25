package com.genericform.engine;

import com.genericform.core.ComponentValidation;
import com.genericform.core.FormComponent;
import com.genericform.core.FormSchema;
import com.genericform.core.SchemaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultFormSchemaManagerTest {

    @Mock
    private SchemaRepository schemaRepository;

    @Mock
    private FormRegistry formRegistry;

    private DefaultFormSchemaManager manager;

    @BeforeEach
    void setUp() {
        manager = new DefaultFormSchemaManager(schemaRepository, formRegistry);
    }

    private FormSchema validSchema() {
        return FormSchema.builder()
                .formId("survey")
                .version("1.0")
                .components(List.of(
                        FormComponent.builder()
                                .key("fullName").type("textfield").input(true)
                                .validate(ComponentValidation.builder().required(true).build())
                                .build()))
                .build();
    }

    // ───────────────────────────── Create ─────────────────────────────

    @Nested
    @DisplayName("createSchema")
    class CreateTests {

        @Test
        @DisplayName("should create a valid schema")
        void createValid() {
            FormSchema schema = validSchema();
            when(schemaRepository.existsById("survey")).thenReturn(false);

            FormSchema result = manager.createSchema(schema);

            assertEquals("survey", result.getFormId());
            verify(schemaRepository).save(schema);
        }

        @Test
        @DisplayName("should reject if formId already exists")
        void createDuplicate() {
            FormSchema schema = validSchema();
            when(schemaRepository.existsById("survey")).thenReturn(true);

            assertThrows(IllegalArgumentException.class, () -> manager.createSchema(schema));
            verify(schemaRepository, never()).save(any());
        }

        @Test
        @DisplayName("should reject schema with blank formId")
        void createBlankId() {
            FormSchema schema = FormSchema.builder()
                    .formId("")
                    .version("1.0")
                    .components(List.of(
                            FormComponent.builder()
                                    .key("x").type("textfield").input(true).build()))
                    .build();

            assertThrows(IllegalArgumentException.class, () -> manager.createSchema(schema));
        }

        @Test
        @DisplayName("should reject schema with no components")
        void createNoComponents() {
            FormSchema schema = FormSchema.builder()
                    .formId("test")
                    .version("1.0")
                    .components(List.of())
                    .build();

            assertThrows(IllegalArgumentException.class, () -> manager.createSchema(schema));
        }

        @Test
        @DisplayName("should reject component with blank type")
        void createBlankType() {
            FormSchema schema = FormSchema.builder()
                    .formId("test")
                    .version("1.0")
                    .components(List.of(
                            FormComponent.builder().key("foo").type("").input(true).build()))
                    .build();

            assertThrows(IllegalArgumentException.class, () -> manager.createSchema(schema));
        }

        @Test
        @DisplayName("should reject input component without key")
        void createInputWithoutKey() {
            FormSchema schema = FormSchema.builder()
                    .formId("test")
                    .version("1.0")
                    .components(List.of(
                            FormComponent.builder().key("").type("textfield").input(true).build()))
                    .build();

            assertThrows(IllegalArgumentException.class, () -> manager.createSchema(schema));
        }
    }

    // ───────────────────────────── Update ─────────────────────────────

    @Nested
    @DisplayName("updateSchema")
    class UpdateTests {

        @Test
        @DisplayName("should update and evict cache")
        void updateValid() {
            FormSchema schema = validSchema();
            when(schemaRepository.existsById("survey")).thenReturn(true);

            manager.updateSchema(schema);

            verify(schemaRepository).save(schema);
            verify(formRegistry).evict("survey");
        }

        @Test
        @DisplayName("should reject update for non-existent schema")
        void updateNotFound() {
            FormSchema schema = validSchema();
            when(schemaRepository.existsById("survey")).thenReturn(false);

            assertThrows(IllegalArgumentException.class, () -> manager.updateSchema(schema));
            verify(schemaRepository, never()).save(any());
        }
    }

    // ───────────────────────────── Delete ─────────────────────────────

    @Nested
    @DisplayName("deleteSchema")
    class DeleteTests {

        @Test
        @DisplayName("should delete and evict cache")
        void deleteValid() {
            when(schemaRepository.existsById("survey")).thenReturn(true);

            manager.deleteSchema("survey");

            verify(schemaRepository).deleteById("survey");
            verify(formRegistry).evict("survey");
        }

        @Test
        @DisplayName("should reject delete for non-existent schema")
        void deleteNotFound() {
            when(schemaRepository.existsById("unknown")).thenReturn(false);

            assertThrows(IllegalArgumentException.class, () -> manager.deleteSchema("unknown"));
            verify(schemaRepository, never()).deleteById(any());
        }
    }

    // ───────────────────────────── Read ────────────────────────────────

    @Nested
    @DisplayName("getSchema / listSchemas")
    class ReadTests {

        @Test
        @DisplayName("should return schema by formId")
        void getById() {
            FormSchema schema = validSchema();
            when(schemaRepository.findById("survey")).thenReturn(Optional.of(schema));

            Optional<FormSchema> result = manager.getSchema("survey");

            assertTrue(result.isPresent());
            assertEquals("survey", result.get().getFormId());
        }

        @Test
        @DisplayName("should return all schemas")
        void listAll() {
            when(schemaRepository.findAll()).thenReturn(List.of(validSchema()));

            List<FormSchema> result = manager.listSchemas();

            assertEquals(1, result.size());
        }
    }
}
