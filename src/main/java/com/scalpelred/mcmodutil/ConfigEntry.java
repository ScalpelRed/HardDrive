package com.scalpelred.mcmodutil;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

public abstract class ConfigEntry<T> {

    protected static final Gson GSON = new Gson();

    private final String name;

    private final T defaultValue;
    private T value;

    private String description = null;

    public ConfigEntry(String name, T defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
        value = defaultValue;
        description = "(no description)";
    }

    public ConfigEntry(String name, T defaultValue, String description) {
        this(name, defaultValue);
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() { return description; }

    public void resetValue() {
        value = defaultValue;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    public abstract JsonElement toJsonElement();

    public void valueFromJsonElement(JsonElement json) {
        value = parseJsonElement(json);
    }

    protected abstract T parseJsonElement(JsonElement json);
}
