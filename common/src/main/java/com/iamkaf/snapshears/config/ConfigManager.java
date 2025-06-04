package com.iamkaf.snapshears.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.iamkaf.snapshears.Constants;
import com.iamkaf.snapshears.platform.Services;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String HEADER_COMMENT = "// SnapShears configuration\n"
            + "// Add item ids like 'minecraft:shears' or prefix a tag with '#' (e.g. '#c:tools/shear').\n";
    private static ShearsConfig config;
    private static Path configPath;

    public static ShearsConfig getConfig() {
        if (config == null) {
            loadConfig();
        }
        return config;
    }

    public static void loadConfig() {
        try {
            if (configPath == null) {
                configPath = Services.PLATFORM.getConfigDirectory().resolve(Constants.MOD_ID + ".json5");
            }
            if (Files.exists(configPath)) {
                try (JsonReader reader = new JsonReader(Files.newBufferedReader(configPath))) {
                    reader.setStrictness(Strictness.LEGACY_STRICT);
                    config = GSON.fromJson(reader, ShearsConfig.class);
                }
            } else {
                config = new ShearsConfig();
                saveConfig();
                Constants.LOG.info("Created default configuration at {}", configPath.toAbsolutePath());
            }
        } catch (Exception e) {
            Constants.LOG.error("Could not read SnapShears config at {}. Using defaults. Please check the file for errors.",
                    configPath == null ? "<unknown>" : configPath.toAbsolutePath());
            Constants.LOG.error("{}", e.getMessage());
            config = new ShearsConfig();
        }
    }

    public static void saveConfig() {
        if (configPath == null) {
            configPath = Services.PLATFORM.getConfigDirectory().resolve(Constants.MOD_ID + ".json5");
        }
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                writer.write(HEADER_COMMENT);
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            Constants.LOG.error("Failed to save SnapShears config to {}. Changes may be lost.", configPath.toAbsolutePath());
            Constants.LOG.error("{}", e.getMessage());
        }
    }
}
