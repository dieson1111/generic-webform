package com.genericform.engine;

import com.genericform.core.*;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for custom JavaScript validation via GraalVM.
 * <p>
 * Covers single-field custom scripts, cross-field validation,
 * error message handling, and edge cases (empty scripts, syntax errors).
 * </p>
 */
class JavaScriptValidationTest {

    private ValidationEngine validationEngine;
    private JavaScriptValidationEngine jsEngine;

    @BeforeEach
    void setUp() {
        jsEngine = new JavaScriptValidationEngine(5);
        validationEngine = new ValidationEngine(jsEngine);
    }

    @AfterEach
    void tearDown() {
        if (jsEngine != null) {
            jsEngine.close();
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private FormSchema buildSchemaWithCustomValidation(String fieldKey,
            String customScript) {
        return buildSchemaWithCustomValidation(fieldKey, customScript, null);
    }

    private FormSchema buildSchemaWithCustomValidation(String fieldKey,
            String customScript,
            String customMessage) {
        FormComponent field = FormComponent.builder()
                .key(fieldKey).type("textfield").input(true)
                .validate(ComponentValidation.builder()
                        .required(true)
                        .custom(customScript)
                        .customMessage(customMessage)
                        .build())
                .build();

        return FormSchema.builder()
                .formId("test")
                .version("1.0")
                .components(List.of(field))
                .build();
    }

    // ───────────────────────── Single-Field Custom Validation ──────────────

    @Nested
    @DisplayName("Single-field custom validation")
    class SingleFieldTests {

        @Test
        @DisplayName("should pass when custom script returns valid = true")
        void customValidationPass() {
            FormSchema schema = buildSchemaWithCustomValidation(
                    "textField",
                    "valid = (input === 'Joe') ? true : 'Your name must be \"Joe\"';");

            Map<String, Object> data = Map.of("textField", "Joe");
            Map<String, String> errors = validationEngine.validate(schema, data);
            assertTrue(errors.isEmpty(), "Expected no errors, got: " + errors);
        }

        @Test
        @DisplayName("should fail when custom script returns error message")
        void customValidationFail() {
            FormSchema schema = buildSchemaWithCustomValidation(
                    "textField",
                    "valid = (input === 'Joe') ? true : 'Your name must be \"Joe\"';");

            Map<String, Object> data = Map.of("textField", "Bob");
            Map<String, String> errors = validationEngine.validate(schema, data);
            assertEquals(1, errors.size());
            assertTrue(errors.containsKey("textField"));
            assertTrue(errors.get("textField").contains("Joe"));
        }

        @Test
        @DisplayName("should use customMessage when provided, rather than JS error")
        void customMessageOverridesJsError() {
            FormSchema schema = buildSchemaWithCustomValidation(
                    "textField",
                    "valid = (input === 'Joe') ? true : 'JS error message';",
                    "Custom override message");

            Map<String, Object> data = Map.of("textField", "Bob");
            Map<String, String> errors = validationEngine.validate(schema, data);
            assertEquals("Custom override message", errors.get("textField"));
        }

        @Test
        @DisplayName("should skip JS validation when custom script is empty")
        void emptyCustomScript() {
            FormSchema schema = buildSchemaWithCustomValidation(
                    "textField", "");

            Map<String, Object> data = Map.of("textField", "anything");
            Map<String, String> errors = validationEngine.validate(schema, data);
            assertTrue(errors.isEmpty());
        }

        @Test
        @DisplayName("should skip JS validation when custom script is null")
        void nullCustomScript() {
            FormSchema schema = buildSchemaWithCustomValidation(
                    "textField", null);

            Map<String, Object> data = Map.of("textField", "anything");
            Map<String, String> errors = validationEngine.validate(schema, data);
            assertTrue(errors.isEmpty());
        }
    }

    // ───────────────────────── Cross-Field Validation ──────────────────────

    @Nested
    @DisplayName("Cross-field custom validation")
    class CrossFieldTests {

        @Test
        @DisplayName("should pass when cross-field condition is met via data binding")
        void crossFieldPass() {
            // confirmPassword field validates against password field
            FormComponent password = FormComponent.builder()
                    .key("password").type("textfield").input(true)
                    .validate(ComponentValidation.builder().required(true).build())
                    .build();

            FormComponent confirmPassword = FormComponent.builder()
                    .key("confirmPassword").type("textfield").input(true)
                    .validate(ComponentValidation.builder()
                            .required(true)
                            .custom("valid = (input === data.password) ? true : 'Passwords must match';")
                            .build())
                    .build();

            FormSchema schema = FormSchema.builder()
                    .formId("test").version("1.0")
                    .components(List.of(password, confirmPassword))
                    .build();

            Map<String, Object> data = new HashMap<>();
            data.put("password", "secret123");
            data.put("confirmPassword", "secret123");

            Map<String, String> errors = validationEngine.validate(schema, data);
            assertTrue(errors.isEmpty(), "Expected no errors, got: " + errors);
        }

        @Test
        @DisplayName("should fail when cross-field condition is not met")
        void crossFieldFail() {
            FormComponent password = FormComponent.builder()
                    .key("password").type("textfield").input(true)
                    .validate(ComponentValidation.builder().required(true).build())
                    .build();

            FormComponent confirmPassword = FormComponent.builder()
                    .key("confirmPassword").type("textfield").input(true)
                    .validate(ComponentValidation.builder()
                            .required(true)
                            .custom("valid = (input === data.password) ? true : 'Passwords must match';")
                            .build())
                    .build();

            FormSchema schema = FormSchema.builder()
                    .formId("test").version("1.0")
                    .components(List.of(password, confirmPassword))
                    .build();

            Map<String, Object> data = new HashMap<>();
            data.put("password", "secret123");
            data.put("confirmPassword", "wrong456");

            Map<String, String> errors = validationEngine.validate(schema, data);
            assertEquals(1, errors.size());
            assertTrue(errors.containsKey("confirmPassword"));
            assertEquals("Passwords must match", errors.get("confirmPassword"));
        }

        @Test
        @DisplayName("should access component metadata from script")
        void accessComponentMetadata() {
            FormSchema schema = buildSchemaWithCustomValidation(
                    "myField",
                    "valid = (component.key === 'myField') ? true : 'Wrong component';");

            Map<String, Object> data = Map.of("myField", "value");
            Map<String, String> errors = validationEngine.validate(schema, data);
            assertTrue(errors.isEmpty());
        }
    }

    // ───────────────────────── Error Handling ──────────────────────────────

    @Nested
    @DisplayName("Error handling for JS scripts")
    class ErrorHandlingTests {

        @Test
        @DisplayName("should handle script syntax errors gracefully")
        void syntaxError() {
            FormSchema schema = buildSchemaWithCustomValidation(
                    "textField",
                    "valid = ((;" // invalid syntax
            );

            Map<String, Object> data = Map.of("textField", "value");
            Map<String, String> errors = validationEngine.validate(schema, data);
            assertEquals(1, errors.size());
            assertTrue(errors.containsKey("textField"));
            assertTrue(errors.get("textField").contains("error"),
                    "Expected error message about script error, got: " + errors.get("textField"));
        }

        @Test
        @DisplayName("should handle script runtime errors gracefully")
        void runtimeError() {
            FormSchema schema = buildSchemaWithCustomValidation(
                    "textField",
                    "valid = undefinedFunction();" // runtime error
            );

            Map<String, Object> data = Map.of("textField", "value");
            Map<String, String> errors = validationEngine.validate(schema, data);
            assertEquals(1, errors.size());
            assertTrue(errors.containsKey("textField"));
        }
    }

    // ───────────────────────── Form.io Sample Schema Test ──────────────────

    @Nested
    @DisplayName("Form.io sample schema with container and custom validation")
    class FormIoSampleTests {

        @Test
        @DisplayName("should validate textField with custom 'must be Joe' rule inside container")
        void containerCustomValidation() {
            // Mirrors the sample schema: container → textField with custom validation
            FormComponent textField = FormComponent.builder()
                    .key("textField").type("textfield").input(true).label("Text Field")
                    .validate(ComponentValidation.builder()
                            .required(true)
                            .custom("valid = (input === 'Joe') ? true : 'Your name must be \"Joe\"';\r\n")
                            .build())
                    .build();

            FormComponent member1 = FormComponent.builder()
                    .key("member1").type("textfield").input(true).label("Member")
                    .validate(ComponentValidation.builder().build())
                    .build();

            FormComponent editGrid = FormComponent.builder()
                    .key("member").type("editgrid").input(true).label("Member")
                    .components(List.of(member1))
                    .build();

            FormComponent container = FormComponent.builder()
                    .key("container").type("container").input(false)
                    .components(List.of(textField, editGrid))
                    .build();

            FormSchema schema = FormSchema.builder()
                    .formId("test_complex")
                    .formName("test_complex")
                    .components(List.of(container))
                    .build();

            // Test 1: "Joe" passes
            Map<String, Object> validData = new HashMap<>();
            validData.put("textField", "Joe");
            validData.put("member", List.of(Map.of("member1", "Alice")));

            Map<String, String> noErrors = validationEngine.validate(schema, validData);
            assertTrue(noErrors.isEmpty(), "Expected no errors for 'Joe', got: " + noErrors);

            // Test 2: "Bob" fails
            Map<String, Object> invalidData = new HashMap<>();
            invalidData.put("textField", "Bob");
            invalidData.put("member", List.of(Map.of("member1", "Alice")));

            Map<String, String> errors = validationEngine.validate(schema, invalidData);
            assertEquals(1, errors.size());
            assertTrue(errors.containsKey("textField"));
            assertTrue(errors.get("textField").contains("Joe"));
        }
    }

    // ───────────────────────── Without JS Engine (disabled) ────────────────

    @Nested
    @DisplayName("Validation without JS engine (disabled)")
    class DisabledJsTests {

        @Test
        @DisplayName("should skip custom validation when JS engine is null")
        void skipCustomWhenJsDisabled() {
            ValidationEngine noJsEngine = new ValidationEngine(); // no-arg = null jsEngine

            FormSchema schema = buildSchemaWithCustomValidation(
                    "textField",
                    "valid = (input === 'Joe') ? true : 'Must be Joe';");

            // This should NOT fail because JS validation is skipped
            Map<String, Object> data = Map.of("textField", "Bob");
            Map<String, String> errors = noJsEngine.validate(schema, data);
            assertTrue(errors.isEmpty(),
                    "Custom JS validation should be skipped when engine is null");
        }
    }
}
