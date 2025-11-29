package com.faketils.config;

import com.faketils.Faketils;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.nio.file.Files;

public class Config {
    public static final Config INSTANCE = new Config();

    private final File configFile = new File(Faketils.configDirectory, "config.json");
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private ConfigData data = new ConfigData();

    public boolean fishingHelper = false;
    public boolean fishingHelperFireVeil = false;
    public boolean fishingHelperFireVeilGalatea = false;
    public boolean debug = false;

    public void initialize() {
        if (configFile.exists()) {
            try {
                String content = Files.readString(configFile.toPath());
                data = gson.fromJson(content, ConfigData.class);
                applyData();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void markDirty() {
        data = new ConfigData(
                fishingHelper,
                fishingHelperFireVeil,
                fishingHelperFireVeilGalatea,
                debug
        );
    }

    public void writeData() {
        try {
            Files.writeString(configFile.toPath(), gson.toJson(data));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void applyData() {
        fishingHelper = data.fishingHelper;
        fishingHelperFireVeil = data.fishingHelperFireVeil;
        fishingHelperFireVeilGalatea = data.fishingHelperFireVeilGalatea;
        debug = data.debug;
    }

    public Screen gui() {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(MinecraftClient.getInstance().currentScreen)
                .setTitle(Text.literal("Faketils Config"))
                .setSavingRunnable(() -> {
                    markDirty();
                    writeData();
                });

        ConfigCategory fishing = builder.getOrCreateCategory(Text.literal("Fishing"));
        ConfigCategory debugCategory = builder.getOrCreateCategory(Text.literal("Debug"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        fishing.addEntry(entryBuilder.startBooleanToggle(Text.literal("Fishing Helper"), fishingHelper)
                .setDefaultValue(false)
                .setTooltip(Text.literal("Enables the fishing helper."))
                .setSaveConsumer(val -> fishingHelper = val)
                .build()
        );

        fishing.addEntry(entryBuilder.startBooleanToggle(Text.literal("Fire Veil Killing"), fishingHelperFireVeil)
                .setDefaultValue(false)
                .setTooltip(Text.literal("Enables the fire veil killing, useful for lava fishing."))
                .setSaveConsumer(val -> fishingHelperFireVeil = val)
                .build()
        );

        fishing.addEntry(entryBuilder.startBooleanToggle(Text.literal("Fire Veil Killing in galatea"), fishingHelperFireVeilGalatea)
                .setDefaultValue(false)
                .setTooltip(Text.literal("Enables the fire veil killing for galatea island, useful for lava fishing."))
                .setSaveConsumer(val -> fishingHelperFireVeilGalatea = val)
                .build()
        );

        debugCategory.addEntry(entryBuilder.startBooleanToggle(Text.literal("Debug"), debug)
                .setDefaultValue(false)
                .setTooltip(Text.literal("Dev stuff."))
                .setSaveConsumer(val -> debug = val)
                .build()
        );

        return builder.build();
    }

    private static class ConfigData {
        public boolean fishingHelper;
        public boolean fishingHelperFireVeil;
        public boolean fishingHelperFireVeilGalatea;
        public boolean debug;

        public ConfigData() {
            this(false, false, false, false);
        }

        public ConfigData(boolean fishingHelper, boolean fishingHelperFireVeil, boolean fishingHelperFireVeilGalatea, boolean debug) {
            this.fishingHelper = fishingHelper;
            this.fishingHelperFireVeil = fishingHelperFireVeil;
            this.fishingHelperFireVeilGalatea = fishingHelperFireVeilGalatea;
            this.debug = debug;
        }
    }
}