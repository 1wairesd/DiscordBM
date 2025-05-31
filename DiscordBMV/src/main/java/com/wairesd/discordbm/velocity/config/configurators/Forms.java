package com.wairesd.discordbm.velocity.config.configurators;

import com.wairesd.discordbm.common.utils.logging.PluginLogger;
import com.wairesd.discordbm.common.utils.logging.Slf4jPluginLogger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class Forms {
    private static final PluginLogger logger = new Slf4jPluginLogger(LoggerFactory.getLogger("DiscordBMV"));
    private static final String FORMS_FILE_NAME = "forms.yml";
    private static Path dataDirectory;
    private static Map<String, FormStructured> forms = new HashMap<>();

    public record FormStructured(String name, String title, List<Field> fields) {
        public record Field(String label, String placeholder, String type, boolean required, String variable) {}
    }

    public static void init(File dataDir) {
        dataDirectory = dataDir.toPath();
        loadForms();
    }

    public static void reload() {
        Map<String, FormStructured> reloadedForms = loadForms();
        logger.info("{} reloaded successfully with {} forms", FORMS_FILE_NAME, reloadedForms.size());
    }

    private static synchronized Map<String, FormStructured> loadForms() {
        try {
            Path formsPath = dataDirectory.resolve(FORMS_FILE_NAME);
            if (!Files.exists(formsPath)) {
                createDefaultFormsFile(formsPath);
            }

            Map<String, FormStructured> newForms = loadFormsFromFile(formsPath);
            forms = Collections.unmodifiableMap(newForms);
            return forms;
        } catch (Exception e) {
            logger.error("Error loading {}: {}", FORMS_FILE_NAME, e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    private static void createDefaultFormsFile(Path formsPath) throws IOException {
        Files.createDirectories(dataDirectory);
        try (InputStream in = Forms.class.getClassLoader().getResourceAsStream(FORMS_FILE_NAME)) {
            if (in != null) {
                Files.copy(in, formsPath);
            } else {
                logger.error("{} not found in resources!", FORMS_FILE_NAME);
            }
        }
    }

    private static Map<String, FormStructured> loadFormsFromFile(Path formsPath) throws IOException {
        if (!Files.exists(formsPath)) {
            throw new FileNotFoundException("YAML forms file not found: " + formsPath);
        }

        try (InputStream in = Files.newInputStream(formsPath)) {
            Yaml yaml = new Yaml();
            Map<String, Object> loaded = yaml.load(in);
            if (loaded == null || !loaded.containsKey("forms")) {
                return Collections.emptyMap();
            }

            Map<String, Object> formsMap = (Map<String, Object>) loaded.get("forms");
            Map<String, FormStructured> result = new HashMap<>();
            for (Map.Entry<String, Object> entry : formsMap.entrySet()) {
                String formName = entry.getKey();
                Map<String, Object> formData = (Map<String, Object>) entry.getValue();
                String title = (String) formData.get("title");
                List<Map<String, Object>> fieldsData = (List<Map<String, Object>>) formData.get("fields");

                List<FormStructured.Field> fields = fieldsData.stream()
                        .map(fieldMap -> new FormStructured.Field(
                                (String) fieldMap.get("label"),
                                (String) fieldMap.getOrDefault("placeholder", ""),
                                (String) fieldMap.get("type"),
                                (boolean) fieldMap.getOrDefault("required", false),
                                (String) fieldMap.get("variable")
                        ))
                        .collect(Collectors.toList());

                result.put(formName, new FormStructured(formName, title, fields));
            }
            return result;
        } catch (ClassCastException | IllegalArgumentException e) {
            throw new IOException("Error parsing forms.yml: " + e.getMessage(), e);
        }
    }

    public static Map<String, FormStructured> getForms() {
        return forms;
    }
}