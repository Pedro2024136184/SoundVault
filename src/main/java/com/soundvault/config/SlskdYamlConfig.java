package com.soundvault.config;

import com.soundvault.util.AppLogger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

public class SlskdYamlConfig {

    private final Path yamlPath;
    private Map<String, Object> data = new LinkedHashMap<>();

    public SlskdYamlConfig() {
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData == null || localAppData.isBlank()) {
            localAppData = System.getProperty("user.home");
        }
        this.yamlPath = Paths.get(localAppData, "slskd", "slskd.yml");
        load();
    }

    public Path getYamlPath() {
        return yamlPath;
    }

    public synchronized void load() {
        try {
            if (!Files.exists(yamlPath)) {
                createDefaultYaml();
            }

            Yaml yaml = new Yaml();
            try (FileInputStream fis = new FileInputStream(yamlPath.toFile())) {
                Object loaded = yaml.load(fis);
                if (loaded instanceof Map<?, ?> map) {
                    this.data = castMap(map);
                } else {
                    this.data = new LinkedHashMap<>();
                }
            }
            AppLogger.log("slskd.yml carregado com sucesso de: " + yamlPath);
        } catch (Exception e) {
            AppLogger.log("Aviso ao ler slskd.yml: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Map<?, ?> raw) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            String key = String.valueOf(entry.getKey());
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nested) {
                result.put(key, castMap(nested));
            } else {
                result.put(key, value);
            }
        }
        return result;
    }

    public synchronized String getProperty(String path) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = data;
        for (int i = 0; i < parts.length - 1; i++) {
            Object nested = current.get(parts[i]);
            if (nested instanceof Map<?, ?> map) {
                current = (Map<String, Object>) map;
            } else {
                return "";
            }
        }
        Object value = current.get(parts[parts.length - 1]);
        return value != null ? String.valueOf(value) : "";
    }

    @SuppressWarnings("unchecked")
    public synchronized void setProperty(String path, Object value) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = data;
        for (int i = 0; i < parts.length - 1; i++) {
            Object nested = current.get(parts[i]);
            if (!(nested instanceof Map)) {
                Map<String, Object> newMap = new LinkedHashMap<>();
                current.put(parts[i], newMap);
                current = newMap;
            } else {
                current = (Map<String, Object>) nested;
            }
        }
        current.put(parts[parts.length - 1], value);
    }

    public synchronized void syncApiKey(String apiKey) {
        setProperty("web.authentication.apiKeys.myapikey.key", apiKey);
        setProperty("web.authentication.apiKeys.myapikey.role", "administrator");
        setProperty("web.authentication.apiKeys.myapikey.cidr", "0.0.0.0/0,::/0");
    }

    public synchronized void save() throws Exception {
        Files.createDirectories(yamlPath.getParent());

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);

        Yaml yaml = new Yaml(options);
        try (FileWriter writer = new FileWriter(yamlPath.toFile())) {
            yaml.dump(data, writer);
        }
        AppLogger.log("slskd.yml atualizado com sucesso.");
    }

    private void createDefaultYaml() {
        try {
            Files.createDirectories(yamlPath.getParent());
            String template = """
                directories:
                  incomplete: ""
                  downloads: ""

                soulseek:
                  username: ""
                  password: ""
                  listenPort: 50300

                web:
                  authentication:
                    disabled: false
                    username: slskd
                    password: slskd
                    apiKeys:
                      myapikey:
                        key: ""
                        role: administrator
                        cidr: "0.0.0.0/0,::/0"

                global:
                  download:
                    slots: 100
                    speedLimit: 2000000
                  upload:
                    slots: 5
                    speedLimit: 2000000
                """;
            Files.writeString(yamlPath, template);
            AppLogger.log("Template base slskd.yml injetado em: " + yamlPath);
        } catch (Exception e) {
            AppLogger.log("Erro ao criar template slskd.yml: " + e.getMessage());
        }
    }
}
