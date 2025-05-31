package com.wairesd.discordbm.velocity.config.configurators;

import com.wairesd.discordbm.velocity.commandbuilder.models.buttons.ButtonConfig;
import com.wairesd.discordbm.velocity.commandbuilder.models.pages.Page;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Pages {
    private static final String CONFIG_FILE_NAME = "pages.yml";
    private static File configFile;
    private static Map<String, Object> rawConfig;

    public static final Map<String, Page> pageMap = new HashMap<>();

    public static void init(File dataDir) {
        configFile = new File(dataDir, CONFIG_FILE_NAME);
        loadPages();
    }

    private static void loadPages() {
        try {
            if (!configFile.exists()) {
                createDefaultConfig();
            }
            Yaml yaml = new Yaml();
            try (FileInputStream inputStream = new FileInputStream(configFile)) {
                rawConfig = yaml.load(inputStream);
            }
            parsePages();
        } catch (Exception e) {
            System.err.println("Error loading pages.yml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void createDefaultConfig() throws IOException {
        configFile.getParentFile().mkdirs();
        try (InputStream inputStream = Pages.class.getClassLoader().getResourceAsStream(CONFIG_FILE_NAME)) {
            if (inputStream != null) {
                Files.copy(inputStream, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Default pages.yml loaded from resources to " + configFile.getPath());
            } else {
                throw new IOException(CONFIG_FILE_NAME + " not found in resources");
            }
        }
    }

    private static void parsePages() {
        pageMap.clear();
        if (rawConfig == null) return;

        List<Map<String, Object>> pages = (List<Map<String, Object>>) rawConfig.get("pages");
        if (pages == null) return;

        for (Map<String, Object> pageData : pages) {
            String id = (String) pageData.get("id");
            String content = (String) pageData.get("content");
            Map<String, Object> embedConfig = (Map<String, Object>) pageData.get("embed");
            List<ButtonConfig> buttons = new ArrayList<>();

            List<Map<String, String>> buttonsList = (List<Map<String, String>>) pageData.get("buttons");
            if (buttonsList != null) {
                for (Map<String, String> btn : buttonsList) {
                    String label = btn.get("label");
                    String targetPage = btn.get("target_page");
                    buttons.add(new ButtonConfig(label, targetPage));
                }
            }
            pageMap.put(id, new Page(id, content, embedConfig, buttons));
        }
    }

    public static void reload() {
        loadPages();
        System.out.println("pages.yml reloaded successfully");
    }
}
