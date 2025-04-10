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

    private final String name;
    private final Logger logger;
    private final File folder;
    private final File configFile;
    private final File docFile;
    private JsonObject jsonRoot;
    private final LinkedHashMap<String, JsonElement> jsonEntries = new LinkedHashMap<>();
    private final LinkedHashMap<String, ConfigEntryHandle<?>> entryHandles = new LinkedHashMap<>();

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

    protected void registerEntryHandle(ConfigEntryHandle<?> entry) {
        entryHandles.put(entry.getName(), entry);
        if (entry.getDescription() != null) hasAnyDoc = true;
    }

    public void loadOrCreate() {
        load();
        if (!isLoaded()) save();
    }

    public void load() {
        if (!configFile.exists()) {
            logger.error("Can't load config \"{}\": file is missing.", name);
            return;
        }

        jsonEntries.clear();
        String content;
        try {
            content = new String(Files.readAllBytes(configFile.toPath()));
        }
        catch (IOException e) {
            logger.error("Can't load config \"{}\": {}", name, e.getMessage());
            return;
        }
        try {
            jsonRoot = JsonParser.parseString(content).getAsJsonObject(); // jsonRoot is the json version of config
        }
        catch (IllegalStateException e) {
            logger.error("Error loading config \"{}\": {}", name, e.getMessage());
            jsonRoot = null;
            return;
        }

        for (Map.Entry<String, JsonElement> pair : jsonRoot.entrySet()) {
            jsonEntries.put(pair.getKey(), pair.getValue()); // making a dictionary of name-(json version of entry value)
        }

        for (Map.Entry<String, ConfigEntryHandle<?>> pair : entryHandles.entrySet()) {
            JsonElement entry = jsonEntries.get(pair.getKey());
            ConfigEntryHandle<?> handle = pair.getValue(); // creating a normal handle for entry
            if (entry == null) handle.resetValue(); // if we don't have this entry in json, we set default value
            else {
                handle.valueFromJsonElement(entry, logger); // otherwise, we set it to value we read
                handle.resetHasChanged();
            }
        }

        logger.info("Config \"{}\" loaded!", name);
    }

    public void save() {
        if (jsonRoot == null) jsonRoot = new JsonObject(); // creating new json version of config

        for (Map.Entry<String, ConfigEntryHandle<?>> pair : entryHandles.entrySet()) {
            ConfigEntryHandle<?> handle = pair.getValue();
            if (!handle.hasChanged()) continue; // no need to update
            String name = pair.getKey();
            if (jsonEntries.get(name) != null) jsonRoot.remove(name); // remove entry with old value
            JsonElement json = handle.toJsonElement();
            jsonRoot.add(name, json); // add entry
            jsonEntries.put(name, json);
            handle.resetHasChanged();
        }

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

        if (configFileExists) {
            try (FileWriter fileWriter = new FileWriter(configFile)) {
                GSON.toJson(jsonRoot, fileWriter);
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
                for (ConfigEntryHandle<?> entry : entryHandles.values()) {
                    String desc = entry.getDescription();
                    if (desc == null) continue;
                    fileWriter.write("=== " + entry.getName() + " (" + entry.defaultValue().getClass().getSimpleName() + ") ===\n");
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
        return jsonRoot != null;
    }
}
