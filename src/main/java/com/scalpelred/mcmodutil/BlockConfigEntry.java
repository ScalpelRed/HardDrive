package com.scalpelred.mcmodutil;

import com.google.gson.JsonElement;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class BlockConfigEntry extends ConfigEntry<Block> {

    private final Config config;

    public BlockConfigEntry(Config config, String name, Block defaultValue) {
        super(name, defaultValue);
        this.config = config;
    }

    public BlockConfigEntry(Config config, String name, Block defaultValue, String description) {
        super(name, defaultValue, description);
        this.config = config;
    }

    @Override
    public JsonElement toJsonElement() {
        return GSON.toJsonTree(getBlockId(this.getValue()), String.class);
    }

    @Override
    protected Block parseJsonElement(JsonElement json) {
        return getBlock(json.getAsString());
    }

    public static String getBlockId(Block block) {
        return Registries.BLOCK.getId(block).toString();
    }

    public static Block getBlock(String name) {
        return Registries.BLOCK.get(Identifier.of(name));
    }
}
