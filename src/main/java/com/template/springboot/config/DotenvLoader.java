package com.template.springboot.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tiny {@code .env} loader called directly from {@code main()} before
 * {@link org.springframework.boot.SpringApplication#run}. This bypasses Spring's
 * {@code EnvironmentPostProcessor} SPI — historically the fragile path in Spring Boot
 * 4.x — and publishes each entry via {@link System#setProperty(String, String)} so the
 * {@code ${…}} placeholder resolver in application.yml can find them on first lookup.
 *
 * <p><b>Search order</b>: current working directory, then walking up to {@value #MAX_PARENT_DEPTH}
 * parent directories. Lets the app find {@code .env} whether launched from the project root
 * ({@code ./mvnw spring-boot:run}) or from an IDE whose working dir is a module subfolder.
 *
 * <p><b>Precedence</b>: existing system properties win — set {@code -DSPRING_MAIL_HOST=…}
 * on the command line and the {@code .env} value is left alone.
 *
 * <p>Parser rules: lines starting with {@code #} are comments; blank lines ignored;
 * leading/trailing whitespace trimmed from keys; values keep internal spaces;
 * surrounding single or double quotes are stripped; inline {@code # comment} is stripped
 * from unquoted values only.
 */
public final class DotenvLoader {

    private static final int MAX_PARENT_DEPTH = 5;

    private DotenvLoader() {}

    /** Loads {@code .env} (if present) into system properties. Safe to call multiple times. */
    public static void load() {
        Path file = locate();
        if (file == null) {
            System.err.println("[dotenv] no .env found near " + Paths.get("").toAbsolutePath()
                    + " — application.yml defaults will be used");
            return;
        }
        Map<String, String> values = new LinkedHashMap<>();
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                parseLine(line, values);
            }
        } catch (IOException ex) {
            System.err.println("[dotenv] failed to read " + file + ": " + ex.getMessage());
            return;
        }
        if (values.isEmpty()) {
            System.err.println("[dotenv] " + file + " parsed but yielded no KEY=VALUE entries");
            return;
        }
        int written = 0;
        for (Map.Entry<String, String> e : values.entrySet()) {
            if (System.getProperty(e.getKey()) == null) {
                System.setProperty(e.getKey(), e.getValue());
                written++;
            }
        }
        System.out.println("[dotenv] loaded " + values.size() + " entries from " + file
                + " (" + written + " new system properties, "
                + (values.size() - written) + " preserved from -D / OS env)");
    }

    private static Path locate() {
        Path current = Paths.get("").toAbsolutePath();
        for (int depth = 0; depth <= MAX_PARENT_DEPTH && current != null; depth++) {
            Path candidate = current.resolve(".env");
            if (Files.isRegularFile(candidate)) return candidate;
            current = current.getParent();
        }
        return null;
    }

    private static void parseLine(String raw, Map<String, String> out) {
        if (raw == null) return;
        String line = raw.trim();
        if (line.isEmpty() || line.startsWith("#")) return;
        int eq = line.indexOf('=');
        if (eq <= 0) return;
        String key = line.substring(0, eq).trim();
        String value = line.substring(eq + 1).trim();
        if (!value.startsWith("\"") && !value.startsWith("'")) {
            int hash = value.indexOf(" #");
            if (hash >= 0) value = value.substring(0, hash).trim();
        } else if (value.length() >= 2
                && ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'")))) {
            value = value.substring(1, value.length() - 1);
        }
        out.put(key, value);
    }
}
