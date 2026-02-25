package com.genericform.engine;

import com.genericform.core.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultFormEngineTest {

    @Mock
    private FormRegistry formRegistry;

    @Mock
    private ValidationEngine validationEngine;

    @Mock
    private FormRepository formRepository;

    private DefaultFormEngine formEngine;

    @BeforeEach
    void setUp() {
        formEngine = new DefaultFormEngine(formRegistry, validationEngine, formRepository);
    }

    private FormSchema sampleSchema() {
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

    @Test
    @DisplayName("should validate and save when data is valid")
    void validSubmission() {
        FormSchema schema = sampleSchema();
        Map<String, Object> data = Map.of("fullName", "John Doe");

        when(formRegistry.get("survey")).thenReturn(Optional.of(schema));
        when(validationEngine.validate(schema, data)).thenReturn(Map.of());

        SubmissionResult result = formEngine.process("survey", data);

        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
        verify(formRepository, times(1)).save("survey", data);
    }

    @Test
    @DisplayName("should return errors and NOT save when data is invalid")
    void invalidSubmission() {
        FormSchema schema = sampleSchema();
        Map<String, Object> data = Map.of();

        when(formRegistry.get("survey")).thenReturn(Optional.of(schema));
        when(validationEngine.validate(schema, data))
                .thenReturn(Map.of("fullName", "Field is required"));

        SubmissionResult result = formEngine.process("survey", data);

        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        verify(formRepository, never()).save(anyString(), anyMap());
    }

    @Test
    @DisplayName("should throw when schema is not found")
    void schemaNotFound() {
        when(formRegistry.get("unknown")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> formEngine.process("unknown", Map.of()));

        verify(formRepository, never()).save(anyString(), anyMap());
    }
}
