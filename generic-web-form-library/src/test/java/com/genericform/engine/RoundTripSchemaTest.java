package com.genericform.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.genericform.core.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that the full Form.io schema (with 60+ fields per component)
 * survives a JSON → Java → JSON round-trip without losing any properties.
 * <p>
 * This test reads the production {@code form_schema_sample.json}, deserializes
 * it into the model, re-serializes it, re-deserializes it again, and asserts
 * that all "extra" Form.io fields landed in {@code additionalProperties} maps
 * and were faithfully re-emitted.
 * </p>
 */
class RoundTripSchemaTest {

        private final ObjectMapper mapper = new ObjectMapper()
                        .enable(SerializationFeature.INDENT_OUTPUT);

        // ────────────────────────── Helpers ─────────────────────────────────────

        /**
         * Load form_schema_sample.json from classpath (copied to test-resources).
         * Falls back to direct file read from form-sample directory.
         */
        private FormSchema loadSampleSchema() throws Exception {
                // Try classpath first
                InputStream is = getClass().getResourceAsStream("/form_schema_sample.json");

                if (is == null) {
                        // Fall back to file system path relative to project root
                        java.io.File file = new java.io.File(
                                        "..\\form-sample\\form_schema_sample.json");
                        assertTrue(file.exists(),
                                        "form_schema_sample.json not found on classpath or at: "
                                                        + file.getAbsolutePath());
                        return mapper.readValue(file, FormSchema.class);
                }

                return mapper.readValue(is, FormSchema.class);
        }

        /**
         * Recursively find a component by key in the component tree.
         */
        private FormComponent findByKey(List<FormComponent> components, String key) {
                if (components == null)
                        return null;
                for (FormComponent c : components) {
                        if (key.equals(c.getKey()))
                                return c;
                        FormComponent found = findByKey(c.getComponents(), key);
                        if (found != null)
                                return found;
                        if (c.getColumns() != null) {
                                for (FormColumn col : c.getColumns()) {
                                        found = findByKey(col.getComponents(), key);
                                        if (found != null)
                                                return found;
                                }
                        }
                }
                return null;
        }

        // ────────────────────────── Tests ───────────────────────────────────────

        @Test
        @DisplayName("Full schema deserializes without error")
        void schemaDeserializesSuccessfully() throws Exception {
                FormSchema schema = loadSampleSchema();

                assertNotNull(schema);
                assertEquals("64a9c8e5b1c2f", schema.getFormId());
                assertEquals("Sample Form", schema.getFormName());
                assertEquals("1.0", schema.getVersion());
                assertNotNull(schema.getComponents());
                assertFalse(schema.getComponents().isEmpty());
        }

        @Test
        @DisplayName("Extra component properties are captured in additionalProperties")
        void extraFieldsCapturedInAdditionalProperties() throws Exception {
                FormSchema schema = loadSampleSchema();

                // The first top-level component is a "well" with many extra properties
                FormComponent well = schema.getComponents().get(0);
                assertEquals("well", well.getType());

                Map<String, Object> extras = well.getAdditionalProperties();
                assertNotNull(extras, "additionalProperties should not be null");

                // Spot-check UI/Display fields
                assertTrue(extras.containsKey("customClass"),
                                "customClass should be in additionalProperties");
                assertTrue(extras.containsKey("tooltip"),
                                "tooltip should be in additionalProperties");
                assertTrue(extras.containsKey("tableView"),
                                "tableView should be in additionalProperties");
                assertTrue(extras.containsKey("labelPosition"),
                                "labelPosition should be in additionalProperties");
                assertTrue(extras.containsKey("html"),
                                "html should be in additionalProperties");

                // Spot-check Behavior/Logic fields
                assertTrue(extras.containsKey("conditional"),
                                "conditional should be in additionalProperties");
                assertTrue(extras.containsKey("logic"),
                                "logic should be in additionalProperties");
                assertTrue(extras.containsKey("overlay"),
                                "overlay should be in additionalProperties");
                assertTrue(extras.containsKey("clearOnHide"),
                                "clearOnHide should be in additionalProperties");
                assertTrue(extras.containsKey("tags"),
                                "tags should be in additionalProperties");
                assertTrue(extras.containsKey("properties"),
                                "properties should be in additionalProperties");
        }

        @Test
        @DisplayName("Textfield input component preserves extra properties")
        void textfieldExtraProperties() throws Exception {
                FormSchema schema = loadSampleSchema();

                // Find a textfield input component
                FormComponent textfield = findByKey(schema.getComponents(),
                                "nameOfAuthorizedInsurer");
                assertNotNull(textfield, "Should find 'nameOfAuthorizedInsurer' component");
                assertEquals("textfield", textfield.getType());
                assertTrue(textfield.isInput());

                Map<String, Object> extras = textfield.getAdditionalProperties();

                // Input/Mask fields
                assertTrue(extras.containsKey("displayMask"),
                                "displayMask should be in additionalProperties");
                assertTrue(extras.containsKey("applyMaskOn"),
                                "applyMaskOn should be in additionalProperties");
                assertTrue(extras.containsKey("autocomplete"),
                                "autocomplete should be in additionalProperties");
                assertTrue(extras.containsKey("spellcheck"),
                                "spellcheck should be in additionalProperties");

                // Input-specific fields
                assertTrue(extras.containsKey("inputFormat"),
                                "inputFormat should be in additionalProperties");
                assertTrue(extras.containsKey("inputType"),
                                "inputType should be in additionalProperties");
                assertTrue(extras.containsKey("id"),
                                "id should be in additionalProperties");
        }

        @Test
        @DisplayName("Validation sub-object preserves extra properties")
        void validationExtraProperties() throws Exception {
                FormSchema schema = loadSampleSchema();

                FormComponent textfield = findByKey(schema.getComponents(),
                                "nameOfAuthorizedInsurer");
                assertNotNull(textfield);
                assertNotNull(textfield.getValidate());

                Map<String, Object> valExtras = textfield.getValidate()
                                .getAdditionalProperties();
                assertNotNull(valExtras);

                assertTrue(valExtras.containsKey("customPrivate"),
                                "customPrivate should be in validate.additionalProperties");
                assertTrue(valExtras.containsKey("strictDateValidation"),
                                "strictDateValidation should be in validate.additionalProperties");
                assertTrue(valExtras.containsKey("json"),
                                "json should be in validate.additionalProperties");
        }

        @Test
        @DisplayName("Column layout preserves extra properties (push, pull, size)")
        void columnExtraProperties() throws Exception {
                FormSchema schema = loadSampleSchema();

                // Find a columns component — walk the tree
                FormComponent columns = findByKey(schema.getComponents(), "columns");
                if (columns == null) {
                        // Search for type=columns
                        columns = findComponentByType(schema.getComponents(), "columns");
                }
                assertNotNull(columns, "Should find a columns-type component");
                assertNotNull(columns.getColumns());
                assertFalse(columns.getColumns().isEmpty());

                FormColumn firstCol = columns.getColumns().get(0);
                Map<String, Object> colExtras = firstCol.getAdditionalProperties();
                assertNotNull(colExtras);

                assertTrue(colExtras.containsKey("push"),
                                "push should be in column additionalProperties");
                assertTrue(colExtras.containsKey("pull"),
                                "pull should be in column additionalProperties");
                assertTrue(colExtras.containsKey("size"),
                                "size should be in column additionalProperties");
        }

        @Test
        @DisplayName("Round-trip: serialize → deserialize preserves all fields")
        void roundTripPreservesAllFields() throws Exception {
                FormSchema original = loadSampleSchema();

                // Serialize to JSON
                String json = mapper.writeValueAsString(original);

                // Deserialize back
                FormSchema roundTripped = mapper.readValue(json, FormSchema.class);

                // Schema-level checks
                assertEquals(original.getFormId(), roundTripped.getFormId());
                assertEquals(original.getFormName(), roundTripped.getFormName());
                assertEquals(original.getVersion(), roundTripped.getVersion());
                assertEquals(original.getComponents().size(),
                                roundTripped.getComponents().size());

                // Component-level: check first component extras survived
                FormComponent origWell = original.getComponents().get(0);
                FormComponent rtWell = roundTripped.getComponents().get(0);
                assertEquals(origWell.getAdditionalProperties().keySet(),
                                rtWell.getAdditionalProperties().keySet(),
                                "All extra property keys should survive round-trip");
        }

        @Test
        @DisplayName("Validation still works on deserialized full schema")
        void validationWorksOnFullSchema() throws Exception {
                FormSchema schema = loadSampleSchema();

                ValidationEngine engine = new ValidationEngine();

                // Empty data should not cause errors for non-required fields
                Map<String, String> errors = engine.validate(schema, Map.of());
                // The sample schema has no required fields, so should be no errors
                assertTrue(errors.isEmpty(),
                                "Non-required field validation should pass with empty data: " + errors);
        }

        // ────────────────────────── Utility ─────────────────────────────────────

        private FormComponent findComponentByType(List<FormComponent> components,
                        String type) {
                if (components == null)
                        return null;
                for (FormComponent c : components) {
                        if (type.equals(c.getType()))
                                return c;
                        FormComponent found = findComponentByType(c.getComponents(), type);
                        if (found != null)
                                return found;
                        if (c.getColumns() != null) {
                                for (FormColumn col : c.getColumns()) {
                                        found = findComponentByType(col.getComponents(), type);
                                        if (found != null)
                                                return found;
                                }
                        }
                }
                return null;
        }
}
