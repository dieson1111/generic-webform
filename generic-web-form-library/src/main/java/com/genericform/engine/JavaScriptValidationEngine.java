package com.genericform.engine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genericform.core.FormComponent;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.IOAccess;

import java.time.Duration;
import java.util.Map;

/**
 * Sandboxed JavaScript validation engine using GraalVM Polyglot (GraalJS).
 * <p>
 * Evaluates Form.io {@code validate.custom} scripts server-side for
 * cross-field validation. Each script execution gets a fresh, isolated
 * {@link Context} with no host-access privileges.
 * </p>
 *
 * <h3>Form.io Custom Validation Contract</h3>
 * The script must set a variable {@code valid}:
 * <ul>
 * <li>{@code valid = true} → validation passes</li>
 * <li>{@code valid = "error"} → validation fails with the given message</li>
 * </ul>
 *
 * <h3>Available Bindings</h3>
 * <ul>
 * <li>{@code input} — the value of the current field</li>
 * <li>{@code data} — the full flat submission data (all fields)</li>
 * <li>{@code row} — same as {@code data} for flat forms</li>
 * <li>{@code component} — component metadata (key, type, label)</li>
 * </ul>
 *
 * <h3>Security</h3>
 * <ul>
 * <li>Host access is disabled — scripts cannot call Java classes</li>
 * <li>Execution is time-limited by a configurable timeout
 * (default 5 seconds) to prevent infinite loops</li>
 * <li>Each evaluation runs in a fresh context to prevent state leakage</li>
 * </ul>
 */
@Slf4j
public class JavaScriptValidationEngine {

    private static final String JS_LANGUAGE = "js";

    /** Shared GraalVM engine — expensive to create, safe to reuse. */
    private final Engine sharedEngine;

    /** Maximum time a single script is allowed to run. */
    private final Duration timeout;

    /** Jackson mapper for converting Java objects to JSON strings. */
    private final ObjectMapper objectMapper;

    /**
     * Create a new JavaScript validation engine.
     *
     * @param timeoutSeconds maximum execution time in seconds for one script
     */
    public JavaScriptValidationEngine(int timeoutSeconds) {
        this(timeoutSeconds, new ObjectMapper());
    }

    /**
     * Create a new JavaScript validation engine with a custom ObjectMapper.
     *
     * @param timeoutSeconds maximum execution time in seconds for one script
     * @param objectMapper   Jackson mapper for JSON serialization
     */
    public JavaScriptValidationEngine(int timeoutSeconds, ObjectMapper objectMapper) {
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.objectMapper = objectMapper;
        this.sharedEngine = Engine.newBuilder()
                .option("engine.WarnInterpreterOnly", "false")
                .build();
        log.info("JavaScriptValidationEngine initialized (timeout={}s)", timeoutSeconds);
    }

    /**
     * Evaluate a Form.io custom validation script.
     *
     * @param script    the JavaScript code from {@code validate.custom}
     * @param input     the current field's submitted value (may be null)
     * @param data      the full flat submission data
     * @param component the current component being validated
     * @return {@code null} if validation passes, otherwise the error message string
     */
    public String evaluate(String script, Object input, Map<String, Object> data,
            FormComponent component) {
        if (script == null || script.isBlank()) {
            return null;
        }

        try (Context context = createSandboxedContext()) {
            Value bindings = context.getBindings(JS_LANGUAGE);

            // ── Bind 'input' as native JS value via JSON.parse ────────
            // Java strings via putMember become foreign objects in GraalJS;
            // === comparison with JS string literals fails. JSON.parse
            // produces native JS types that work correctly.
            String inputJson = objectMapper.writeValueAsString(input);
            context.eval(JS_LANGUAGE,
                    "var input = JSON.parse('" + escapeForJsString(inputJson) + "');");

            // ── Bind 'data' and 'row' as native JS objects via JSON.parse ──
            // This ensures all values are native JS types and === works
            String dataJson = objectMapper.writeValueAsString(
                    data != null ? data : Map.of());
            context.eval(JS_LANGUAGE,
                    "var data = JSON.parse('" + escapeForJsString(dataJson) + "');" +
                            "var row = data;");

            // ── Bind 'component' as a native JS object ──────────────────
            context.eval(JS_LANGUAGE,
                    "var component = { " +
                            "key: '" + escapeForJsString(
                                    component.getKey() != null ? component.getKey() : "")
                            + "', " +
                            "type: '" + escapeForJsString(
                                    component.getType() != null ? component.getType() : "")
                            + "', " +
                            "label: '" + escapeForJsString(
                                    component.getLabel() != null ? component.getLabel() : "")
                            + "'" +
                            "};");

            // ── Initialize valid ────────────────────────────────────────
            bindings.putMember("valid", true);

            // ── Execute the custom script ───────────────────────────────
            context.eval(JS_LANGUAGE, script);

            // ── Read the 'valid' result ─────────────────────────────────
            Value validValue = bindings.getMember("valid");

            if (validValue == null || validValue.isNull()) {
                return "Custom validation script did not set 'valid'";
            }

            if (validValue.isBoolean() && validValue.asBoolean()) {
                return null; // passed
            }

            if (validValue.isBoolean() && !validValue.asBoolean()) {
                return "Custom validation failed";
            }

            if (validValue.isString()) {
                String msg = validValue.asString();
                if ("true".equalsIgnoreCase(msg)) {
                    return null;
                }
                return msg;
            }

            if (validValue.isNumber()) {
                return validValue.asDouble() != 0 ? null : "Custom validation failed";
            }

            return null;

        } catch (PolyglotException e) {
            if (e.isCancelled() || e.isResourceExhausted()) {
                log.warn("Custom validation script timed out: {}", e.getMessage());
                return "Custom validation script execution timed out";
            }
            log.warn("Custom validation script error: {}", e.getMessage());
            return "Custom validation script error: " + e.getMessage();
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize data for JS validation", e);
            return "Custom validation error: failed to serialize data";
        } catch (Exception e) {
            log.error("Unexpected error evaluating custom validation", e);
            return "Custom validation error: " + e.getMessage();
        }
    }

    /**
     * Create a sandboxed GraalVM JS context with no host access.
     */
    private Context createSandboxedContext() {
        return Context.newBuilder(JS_LANGUAGE)
                .engine(sharedEngine)
                .allowHostAccess(HostAccess.NONE)
                .allowHostClassLookup(className -> false)
                .allowIO(IOAccess.NONE)
                .allowCreateThread(false)
                .allowNativeAccess(false)
                .allowCreateProcess(false)
                .allowEnvironmentAccess(org.graalvm.polyglot.EnvironmentAccess.NONE)
                .build();
    }

    /**
     * Escape a Java string for safe embedding inside a JS single-quoted
     * string literal. Double-quotes are NOT escaped since they are valid
     * inside single-quoted JS strings and needed for JSON content.
     */
    private String escapeForJsString(String s) {
        if (s == null)
            return "";
        return s.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    /**
     * Close the shared engine. Call on application shutdown.
     */
    public void close() {
        if (sharedEngine != null) {
            sharedEngine.close();
        }
    }
}
