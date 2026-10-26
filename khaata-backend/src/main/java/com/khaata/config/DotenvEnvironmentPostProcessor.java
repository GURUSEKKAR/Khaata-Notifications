package com.khaata.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Loads a {@code .env} file (simple KEY=VALUE lines) and makes the entries available
 * as Spring properties, so placeholders like {@code ${TWILIO_ACCOUNT_SID}} resolve
 * from it. The file is searched in the working directory and its parent, so it works
 * whether the app is started from the module folder or the repo root.
 *
 * <p>Registered via META-INF/spring.factories. Values already present in the real OS
 * environment take precedence (this source is added with lower priority), so you can
 * still override any key with an actual env var.
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (Path candidate : List.of(Paths.get(".env"), Paths.get("..", ".env"),
                Paths.get("khaata-backend", ".env"))) {
            if (Files.isRegularFile(candidate)) {
                parseInto(candidate, values);
                break;
            }
        }
        if (!values.isEmpty()) {
            // addLast => lower precedence than real OS env vars and command-line args.
            environment.getPropertySources().addLast(new MapPropertySource("dotenv", values));
        }

        // Normalize a Postgres URL that some hosts (e.g. Render) provide in the
        // "postgresql://user:pass@host:port/db" form into the JDBC form Spring needs.
        // Added as a HIGH-priority source so it overrides the raw env var.
        Map<String, Object> normalized = normalizePostgresUrl(environment);
        if (!normalized.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource("db-url-normalized", normalized));
        }
    }

    /** @return a map of corrected DB_* properties, or empty if no change needed. */
    private Map<String, Object> normalizePostgresUrl(ConfigurableEnvironment env) {
        Map<String, Object> out = new LinkedHashMap<>();
        String dbUrl = env.getProperty("DB_URL");
        if (dbUrl == null) {
            return out;
        }
        String trimmed = dbUrl.trim();
        if (trimmed.startsWith("postgres://") || trimmed.startsWith("postgresql://")) {
            try {
                String withoutScheme = trimmed.substring(trimmed.indexOf("://") + 3);
                String creds = null;
                String hostAndPath = withoutScheme;
                int at = withoutScheme.indexOf('@');
                if (at >= 0) {
                    creds = withoutScheme.substring(0, at);
                    hostAndPath = withoutScheme.substring(at + 1);
                }
                out.put("DB_URL", "jdbc:postgresql://" + hostAndPath);
                if (creds != null && env.getProperty("DB_USERNAME") == null) {
                    int colon = creds.indexOf(':');
                    out.put("DB_USERNAME", colon >= 0 ? creds.substring(0, colon) : creds);
                    out.put("DB_PASSWORD", colon >= 0 ? creds.substring(colon + 1) : "");
                }
            } catch (RuntimeException e) {
                System.err.println("Could not normalize DB_URL: " + e.getMessage());
            }
        }
        return out;
    }

    private void parseInto(Path file, Map<String, Object> out) {
        try {
            for (String raw : Files.readAllLines(file)) {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                    continue;
                }
                int eq = line.indexOf('=');
                String key = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();
                // Strip surrounding quotes if present.
                if (value.length() >= 2
                        && ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'")))) {
                    value = value.substring(1, value.length() - 1);
                }
                if (!key.isEmpty()) {
                    out.put(key, value);
                }
            }
        } catch (IOException e) {
            // Non-fatal: if the .env can't be read, fall back to defaults/real env.
            System.err.println("Could not read .env: " + e.getMessage());
        }
    }
}
