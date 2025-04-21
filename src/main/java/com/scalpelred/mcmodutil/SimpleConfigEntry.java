package com.scalpelred.mcmodutil;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;

public class SimpleConfigEntry<T> extends ConfigEntry<T> {

    private final Class<T> type;
    private final Config config;

    public SimpleConfigEntry(Config config, String name, Class<T> type, T defaultValue) {
        super(name, defaultValue);
        this.type = type;
        this.config = config;
    }

    public SimpleConfigEntry(Config config, String name, Class<T> type, T defaultValue, String description) {
        super(name, defaultValue, description);
        this.type = type;
        this.config = config;
    }

    @Override
    public JsonElement toJsonElement() {
        return GSON.toJsonTree(this.getValue(), type);
    }

    @Override
    protected T parseJsonElement(JsonElement json) {
        try {
            return GSON.fromJson(json, type);
        }
        catch (JsonSyntaxException e) {
            config.logger.error("Config \"{}\", entry \"{}\": cannot parse value to {}",
                    config.getName(), this.getName(), type.getName());
            return getDefaultValue();
        }
    }
}
