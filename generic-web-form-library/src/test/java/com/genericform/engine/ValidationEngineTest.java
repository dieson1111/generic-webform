package com.genericform.engine;

import com.genericform.core.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValidationEngineTest {

        private ValidationEngine validationEngine;

        @BeforeEach
        void setUp() {
                validationEngine = new ValidationEngine();
        }

        // ── Helper: build a ComponentValidation ──────────────────────────────

        private ComponentValidation requiredValidation() {
                return ComponentValidation.builder().required(true).build();
        }

        private ComponentValidation optionalValidation() {
                return ComponentValidation.builder().required(false).build();
        }

        private ComponentValidation patternValidation(String pattern) {
                return ComponentValidation.builder().required(true).pattern(pattern).build();
        }

        // ── Helper: wrap input component inside a layout (well → fieldset) ──

        private FormSchema wrapInLayout(FormComponent... inputComponents) {
                FormComponent fieldset = FormComponent.builder()
                                .type("fieldset")
                                .key("fieldSet1")
                                .input(false)
                                .components(List.of(inputComponents))
                                .build();

                FormComponent well = FormComponent.builder()
                                .type("well")
                                .key("well1")
                                .input(false)
                                .components(List.of(fieldset))
                                .build();

                return FormSchema.builder()
                                .formId("test")
                                .version("1.0")
                                .components(List.of(well))
                                .build();
        }

        // ───────────────────────────── Required Field ─────────────────────────

        @Nested
        @DisplayName("Required field validation")
        class RequiredFieldTests {

                @Test
                @DisplayName("should fail when a required textfield is missing (nested in well → fieldset)")
                void missingRequiredField() {
                        FormSchema schema = wrapInLayout(
                                        FormComponent.builder()
                                                        .key("username").type("textfield").input(true)
                                                        .validate(requiredValidation()).build());

                        Map<String, String> errors = validationEngine.validate(schema, Map.of());
                        assertEquals(1, errors.size());
                        assertTrue(errors.containsKey("username"));
                }

                @Test
                @DisplayName("should pass when a required textfield is provided")
                void presentRequiredField() {
                        FormSchema schema = wrapInLayout(
                                        FormComponent.builder()
                                                        .key("username").type("textfield").input(true)
                                                        .validate(requiredValidation()).build());

                        Map<String, String> errors = validationEngine.validate(schema, Map.of("username", "john"));
                        assertTrue(errors.isEmpty());
                }

                @Test
                @DisplayName("should pass when an optional field is missing")
                void missingOptionalField() {
                        FormSchema schema = wrapInLayout(
                                        FormComponent.builder()
                                                        .key("nickname").type("textfield").input(true)
                                                        .validate(optionalValidation()).build());

                        Map<String, String> errors = validationEngine.validate(schema, Map.of());
                        assertTrue(errors.isEmpty());
                }
        }

        // ───────────────────────────── Regex / Pattern ────────────────────────

        @Nested
        @DisplayName("Pattern validation")
        class PatternTests {

                @Test
                @DisplayName("should fail when value does not match pattern")
                void patternMismatch() {
                        FormSchema schema = wrapInLayout(
                                        FormComponent.builder()
                                                        .key("postalCode").type("textfield").input(true)
                                                        .validate(patternValidation("^[0-9]{5}$")).build());

                        Map<String, String> errors = validationEngine.validate(schema, Map.of("postalCode", "ABC"));
                        assertEquals(1, errors.size());
                        assertTrue(errors.get("postalCode").contains("pattern"));
                }

                @Test
                @DisplayName("should pass when value matches pattern")
                void patternMatch() {
                        FormSchema schema = wrapInLayout(
                                        FormComponent.builder()
                                                        .key("postalCode").type("textfield").input(true)
                                                        .validate(patternValidation("^[0-9]{5}$")).build());

                        Map<String, String> errors = validationEngine.validate(schema, Map.of("postalCode", "12345"));
                        assertTrue(errors.isEmpty());
                }
        }

        // ───────────────────────────── Email ──────────────────────────────────

        @Nested
        @DisplayName("Email validation")
        class EmailTests {

                @Test
                @DisplayName("should fail for invalid email format")
                void invalidEmail() {
                        FormSchema schema = wrapInLayout(
                                        FormComponent.builder()
                                                        .key("email").type("email").input(true)
                                                        .validate(requiredValidation()).build());

                        Map<String, String> errors = validationEngine.validate(schema, Map.of("email", "not-an-email"));
                        assertFalse(errors.isEmpty());
                }

                @Test
                @DisplayName("should pass for valid email format")
                void validEmail() {
                        FormSchema schema = wrapInLayout(
                                        FormComponent.builder()
                                                        .key("email").type("email").input(true)
                                                        .validate(requiredValidation()).build());

                        Map<String, String> errors = validationEngine.validate(schema,
                                        Map.of("email", "user@example.com"));
                        assertTrue(errors.isEmpty());
                }
        }

        // ───────────────────────────── Select ─────────────────────────────────

        @Nested
        @DisplayName("Select validation")
        class SelectTests {

                private FormComponent buildSelect() {
                        return FormComponent.builder()
                                        .key("role").type("select").input(true)
                                        .validate(requiredValidation())
                                        .dataSrc("values")
                                        .data(ComponentData.builder()
                                                        .values(List.of(
                                                                        SelectValue.builder().label("Chief Executive")
                                                                                        .value("chiefExecutive")
                                                                                        .build(),
                                                                        SelectValue.builder().label("Director")
                                                                                        .value("director").build(),
                                                                        SelectValue.builder().label("Company Secretary")
                                                                                        .value("companySecretary")
                                                                                        .build()))
                                                        .build())
                                        .build();
                }

                @Test
                @DisplayName("should pass when submitted value is one of the allowed options")
                void validSelectValue() {
                        FormSchema schema = wrapInLayout(buildSelect());
                        Map<String, String> errors = validationEngine.validate(schema, Map.of("role", "director"));
                        assertTrue(errors.isEmpty());
                }

                @Test
                @DisplayName("should fail when submitted value is not one of the allowed options")
                void invalidSelectValue() {
                        FormSchema schema = wrapInLayout(buildSelect());
                        Map<String, String> errors = validationEngine.validate(schema, Map.of("role", "invalidRole"));
                        assertEquals(1, errors.size());
                        assertTrue(errors.containsKey("role"));
                }
        }

        // ───────────────────────────── Columns Layout ─────────────────────────

        @Nested
        @DisplayName("Columns layout traversal")
        class ColumnsTests {

                @Test
                @DisplayName("should validate input components inside columns")
                void columnsTraversal() {
                        FormComponent telNo = FormComponent.builder()
                                        .key("businessTelNo").type("phoneNumber").input(true)
                                        .validate(requiredValidation())
                                        .inputMask("9999-9999")
                                        .build();

                        FormComponent faxNo = FormComponent.builder()
                                        .key("faxNo").type("phoneNumber").input(true)
                                        .validate(requiredValidation())
                                        .inputMask("9999-9999")
                                        .build();

                        FormComponent columns = FormComponent.builder()
                                        .key("columns").type("columns").input(false)
                                        .columns(List.of(
                                                        FormColumn.builder()
                                                                        .components(List.of(telNo)).width(6).build(),
                                                        FormColumn.builder()
                                                                        .components(List.of(faxNo)).width(6).build()))
                                        .build();

                        FormSchema schema = FormSchema.builder()
                                        .formId("test").version("1.0")
                                        .components(List.of(columns))
                                        .build();

                        // Both missing
                        Map<String, String> errors = validationEngine.validate(schema, Map.of());
                        assertEquals(2, errors.size());
                        assertTrue(errors.containsKey("businessTelNo"));
                        assertTrue(errors.containsKey("faxNo"));

                        // Both present
                        Map<String, String> noErrors = validationEngine.validate(schema,
                                        Map.of("businessTelNo", "1234-5678", "faxNo", "8765-4321"));
                        assertTrue(noErrors.isEmpty());
                }
        }

        // ───────────────────────────── Static Components ──────────────────────

        @Nested
        @DisplayName("Static component handling")
        class StaticTests {

                @Test
                @DisplayName("should skip content and button components")
                void skipStaticComponents() {
                        FormComponent content = FormComponent.builder()
                                        .key("content1").type("content").input(false).build();

                        FormComponent button = FormComponent.builder()
                                        .key("submit").type("button").input(true).build();

                        FormSchema schema = FormSchema.builder()
                                        .formId("test").version("1.0")
                                        .components(List.of(content, button))
                                        .build();

                        Map<String, String> errors = validationEngine.validate(schema, Map.of());
                        assertTrue(errors.isEmpty(), "Static components should not produce errors");
                }
        }

        // ───────────────────────────── Full Form Sample ───────────────────────

        @Test
        @DisplayName("should validate a Form.io-style schema with flat submission data")
        void fullFormIoHappyPath() {
                // Build a mini Form.io schema: well → fieldset → [textfield, email, columns →
                // phoneNumber]
                FormComponent nameField = FormComponent.builder()
                                .key("nameOfAuthorizedInsurer").type("textfield").input(true)
                                .validate(requiredValidation()).build();

                FormComponent emailField = FormComponent.builder()
                                .key("eMailAddress").type("email").input(true)
                                .validate(optionalValidation()).build();

                FormComponent telNo = FormComponent.builder()
                                .key("businessTelNo").type("phoneNumber").input(true)
                                .validate(optionalValidation()).build();

                FormComponent columns = FormComponent.builder()
                                .key("columns").type("columns").input(false)
                                .columns(List.of(
                                                FormColumn.builder().components(List.of(telNo)).width(6).build()))
                                .build();

                FormComponent signatureField = FormComponent.builder()
                                .key("signature").type("signature").input(true)
                                .validate(optionalValidation()).build();

                FormComponent contentBlock = FormComponent.builder()
                                .key("content1").type("content").input(false).build();

                FormComponent fieldset = FormComponent.builder()
                                .key("fieldSet").type("fieldset").input(false)
                                .components(List.of(contentBlock, nameField, emailField, columns, signatureField))
                                .build();

                FormComponent well = FormComponent.builder()
                                .key("well1").type("well").input(false)
                                .components(List.of(fieldset))
                                .build();

                FormSchema schema = FormSchema.builder()
                                .formId("sampleForm")
                                .formName("Sample Form")
                                .version("1.0")
                                .components(List.of(well))
                                .build();

                // Flat submission data
                Map<String, Object> data = new HashMap<>();
                data.put("nameOfAuthorizedInsurer", "Kinetix Insurance");
                data.put("eMailAddress", "info@kinetix.com");
                data.put("businessTelNo", "1234-5678");
                data.put("signature", "data:image/png;base64,iVBOR...");

                Map<String, String> errors = validationEngine.validate(schema, data);
                assertTrue(errors.isEmpty(), "Expected no errors for valid submission, got: " + errors);
        }
}
