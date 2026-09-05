package com.ailudick.capitalismmod.business;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Complete business-scope directory imported from the supplied official
 * standardized business-scope directory snapshot.
 */
public final class BusinessScopeCatalog {
    public record Scope(String code, String displayName, String industry, String sourceText) {
    }

    private static final List<Scope> ENTRIES = load();

    private BusinessScopeCatalog() {
    }

    public static List<Scope> all() {
        return ENTRIES;
    }

    private static List<Scope> load() {
        List<Scope> entries = new ArrayList<>();
        try (var stream = BusinessScopeCatalog.class.getResourceAsStream(
                "/data/capitalismmod/business_scope_catalog.tsv")) {
            if (stream == null) {
                throw new IllegalStateException("Missing business_scope_catalog.tsv");
            }
            try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank() || line.startsWith("#")) {
                        continue;
                    }
                    String[] columns = line.split("\\t", 4);
                    if (columns.length == 4 && !columns[0].isBlank()) {
                        entries.add(new Scope(columns[0], columns[1], columns[2], columns[3]));
                    }
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load business scope catalog", exception);
        }
        return List.copyOf(entries);
    }
}
