package com.genericform.autoconfigure;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Generic Web Form Engine.
 * <p>
 * Prefix: {@code genericform.schema}
 * </p>
 *
 * <h3>Example — classpath (default):</h3>
 * 
 * <pre>
 * genericform.schema.source=classpath
 * genericform.schema.classpath-prefix=forms/
 * </pre>
 *
 * <h3>Example — filesystem:</h3>
 * 
 * <pre>
 * genericform.schema.source=filesystem
 * genericform.schema.filesystem-path=/opt/forms
 * </pre>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "genericform.schema")
public class GenericFormProperties {

    /**
     * Schema source type.
     * <ul>
     * <li>{@code CLASSPATH} (default) — reads from classpath resources</li>
     * <li>{@code FILESYSTEM} — reads from a filesystem directory</li>
     * </ul>
     */
    private SchemaSource source = SchemaSource.CLASSPATH;

    /**
     * Classpath prefix for schema JSON files (used when {@code source=classpath}).
     * Defaults to {@code "forms/"}.
     */
    private String classpathPrefix = "forms/";

    /**
     * Filesystem directory path for schema JSON files (used when
     * {@code source=filesystem}).
     * Example: {@code /opt/forms} or {@code C:/schemas/forms}
     */
    private String filesystemPath;

    /**
     * Enum defining the supported schema source types.
     */
    public enum SchemaSource {
        CLASSPATH,
        FILESYSTEM,
        REPOSITORY
    }
}
