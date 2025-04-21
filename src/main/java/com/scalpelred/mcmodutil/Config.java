package com.scalpelred.mcmodutil;

import com.google.gson.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;

public abstract class Config {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public final Logger logger;

    private final String name;
    private final File folder;
    private final File configFile;
    private final File docFile;
    private boolean isLoaded;
    private final LinkedHashMap<String, ConfigEntry<?>> entries = new LinkedHashMap<>();

    private String generalDoc = null;
    private boolean hasAnyDoc = false;

    public Config(Logger logger, String name, boolean useSubfolder) {
        this.logger = logger;
        this.name = name;

        if (useSubfolder) {
            String fileName = logger.getName();
            folder = Paths.get(System.getProperty("user.dir"), "config", fileName).toFile();
            configFile = Path.of(folder.getPath(), name + ".json").toFile();
            docFile = Path.of(folder.getPath(), name + "_doc.txt").toFile();
        }
        else {
            folder = Paths.get(System.getProperty("user.dir"), "config").toFile();
            String fileName = logger.getName() + "_" + name;
            configFile = Path.of(folder.getPath(), fileName + ".json").toFile();
            docFile = Path.of(folder.getPath(), fileName + "_doc.txt").toFile();
        }
    }

    public Config(Logger logger, String name, boolean useSubfolder, String generalDoc) {
        this(logger, name, useSubfolder);
        this.generalDoc = generalDoc;
        hasAnyDoc = generalDoc == null;
    }

    protected void registerEntryHandle(ConfigEntry<?> entry) {
        entries.put(entry.getName(), entry);
        if (entry.getDescription() != null) hasAnyDoc = true;
    }

    public boolean loadOrCreate() {
        load();
        if (!isLoaded()) {
            save();
            return true;
        }
        return false;
    }

    public void load() {
        isLoaded = false;

        if (!configFile.exists()) {
            logger.error("Can't load config \"{}\": file is missing.", name);
            return;
        }

        String content;
        try {
            content = new String(Files.readAllBytes(configFile.toPath()));
        }
        catch (IOException e) {
            logger.error("Can't load config \"{}\": {}", name, e.getMessage());
            return;
        }
        JsonObject root;
        try {
            root = JsonParser.parseString(content).getAsJsonObject();
        }
        catch (IllegalStateException e) {
            logger.error("Error loading config \"{}\": {}", name, e.getMessage());
            return;
        }

        for (ConfigEntry<?> entry : entries.values()) entry.resetValue();
        for (Map.Entry<String, JsonElement> json : root.entrySet()) {
            ConfigEntry<?> entry = entries.get(json.getKey());
            if (entry != null) entry.valueFromJsonElement(json.getValue());
        }

        isLoaded = true;
        logger.info("Config \"{}\" loaded!", name);
    }

    public void save() {
        if (!folder.exists()) {
            try {
                Files.createDirectories(folder.toPath());
            }
            catch (IOException e) {
                logger.error("Can't create config directory for \"{}\": {}", name, e.getMessage());
                return;
            }
        }

        boolean configFileExists;
        if (!configFile.exists()) {
            try {
                Files.createFile(configFile.toPath());
                configFileExists = true;
            }
            catch (IOException e) {
                logger.error("Can't save config \"{}\": {}", name, e.getMessage());
                configFileExists = false;
            }
        }
        else configFileExists = true;

        JsonObject root = new JsonObject();
        for (ConfigEntry<?> entry : entries.values()) root.add(entry.getName(), entry.toJsonElement());
        if (configFileExists) {
            try (FileWriter fileWriter = new FileWriter(configFile)) {
                GSON.toJson(root, fileWriter);
            }
            catch (IOException e) {
                logger.error("Can't save config \"{}\": {}", name, e.getMessage());
            }
        }

        if (hasAnyDoc && !docFile.exists()) {
            try {
                Files.createFile(docFile.toPath());
                FileWriter fileWriter = new FileWriter(docFile);
                if (generalDoc != null) {
                    fileWriter.write(generalDoc);
                    fileWriter.append("\n\n");
                }
                for (ConfigEntry<?> entry : entries.values()) {
                    String desc = entry.getDescription();
                    if (desc == null) continue;
                    fileWriter.write("=== " + entry.getName() + " (" + entry.getDefaultValue().getClass().getSimpleName() + ") ===\n");
                    fileWriter.write(desc);
                    fileWriter.write("\n\n");
                }
                fileWriter.close();
            }
            catch (IOException e) {
                logger.error("Can't save config \"{}\" documentation: {}", name, e.getMessage());
            }
        }

        logger.info("Config \"{}\" saved!", name);
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    public String getName() { return name; }
}
