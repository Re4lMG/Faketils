package com.faketils.config;

import com.faketils.Faketils;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.nio.file.Files;

public class Config {
    public static final Config INSTANCE = new Config();

    private File configFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private ConfigData data = new ConfigData();

    public boolean funnyToggle = false;
    public int farmType = 0; // 0 = Melon&Pumpkin..., 1 = Cane/Rose..., 2 = Cocoa Beans
    public boolean funnyWaypoints = false;
    public boolean instaSwitch = false;

    public static final KeyBinding.Category FAKETILS_CATEGORY = new KeyBinding.Category(
            Identifier.of("faketils", "key_category_faketils")
    );

    public static final KeyBinding toggleMacro = new KeyBinding(
            "key.faketils.toggle_macro",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_F8,
            FAKETILS_CATEGORY
    );

    public static final KeyBinding pauseMacro = new KeyBinding(
            "key.faketils.pause_macro",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            FAKETILS_CATEGORY
    );

    public static final KeyBinding resetFakeFails = new KeyBinding(
            "key.faketils.reset_fails",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_BACKSPACE,
            FAKETILS_CATEGORY
    );

    public boolean pestHelper = false;
    public boolean pestFarming = false;
    public int petSwapType = 0; // 0 rod, 1 wardrobe slot
    public int wardrobeSlot = 1;

    public boolean rewarpOnPause = false;

    public boolean fishingHelper = false;
    public boolean slugFishing = false;
    public boolean fishingHelperKilling = false;
    public int fishingHelperKillingAmount = 0; // 0=1, 1=2, 2=3
    public String fishingHelperKillingWeapon = "";

    public int macroHudX = 60;
    public int macroHudY = 200;

    public boolean noHurtCam = false;
    public boolean sphinxSolver = false;
    public boolean fullBlockPanes = false;

    public boolean fishingHelperFireVeil = false;
    public boolean fishingHelperFireVeilGalatea = false;
    public boolean debug = false;

    public void initialize() {
        configFile = new File(Faketils.configDirectory, "config.json");

        KeyBindingHelper.registerKeyBinding(toggleMacro);
        KeyBindingHelper.registerKeyBinding(pauseMacro);
        KeyBindingHelper.registerKeyBinding(resetFakeFails);

        if (configFile.exists()) {
            try {
                String content = Files.readString(configFile.toPath());
                data = gson.fromJson(content, ConfigData.class);
                if (data != null) applyData();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void markDirty() {
        data = new ConfigData(
                funnyToggle, farmType, funnyWaypoints, instaSwitch,
                pestHelper, pestFarming, petSwapType, wardrobeSlot, rewarpOnPause,
                fishingHelper, slugFishing, fishingHelperKilling, fishingHelperKillingAmount, fishingHelperKillingWeapon,
                noHurtCam, sphinxSolver, fullBlockPanes,
                fishingHelperFireVeil, fishingHelperFireVeilGalatea, debug,
                macroHudX, macroHudY
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
        funnyToggle = data.funnyToggle;
        farmType = data.farmType;
        funnyWaypoints = data.funnyWaypoints;
        instaSwitch = data.instaSwitch;

        pestHelper = data.pestHelper;
        pestFarming = data.pestFarming;
        petSwapType = data.petSwapType;
        wardrobeSlot = data.wardrobeSlot;
        rewarpOnPause = data.rewarpOnPause;

        fishingHelper = data.fishingHelper;
        slugFishing = data.slugFishing;
        fishingHelperKilling = data.fishingHelperKilling;
        fishingHelperKillingAmount = data.fishingHelperKillingAmount;
        fishingHelperKillingWeapon = data.fishingHelperKillingWeapon != null ? data.fishingHelperKillingWeapon : "";

        noHurtCam = data.noHurtCam;
        sphinxSolver = data.sphinxSolver;
        fullBlockPanes = data.fullBlockPanes;

        macroHudX = data.macroHudX;
        macroHudY = data.macroHudY;

        fishingHelperFireVeil = data.fishingHelperFireVeil;
        fishingHelperFireVeilGalatea = data.fishingHelperFireVeilGalatea;
        debug = data.debug;
    }

    public Screen gui() {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(MinecraftClient.getInstance().currentScreen)
                .setTitle(Text.literal("Faketils Config"))
                .setSavingRunnable(() -> {Faketils.saveAll();});

        ConfigEntryBuilder entry = builder.entryBuilder();

        // Farming Category
        ConfigCategory farming = builder.getOrCreateCategory(Text.literal("Farming"));

        // Funny Subcategory
        farming.addEntry(entry.startBooleanToggle(Text.literal("Funny Toggle"), funnyToggle)
                .setDefaultValue(false)
                .setTooltip(Text.literal("Display if the funny is active or not. Disable if you aren't farming."))
                .setSaveConsumer(val -> funnyToggle = val)
                .build());

        farming.addEntry(entry.startEnumSelector(Text.literal("Farm Type"), FarmType.class, FarmType.values()[farmType])
                .setDefaultValue(FarmType.MELON_PUMPKIN)
                .setTooltip(Text.literal("Select a farm."))
                .setSaveConsumer(val -> farmType = val.ordinal())
                .build());

        farming.addEntry(entry.startBooleanToggle(Text.literal("Show Waypoints"), funnyWaypoints)
                .setDefaultValue(false)
                .setTooltip(Text.literal("Display the lane switching waypoints. Disable if you aren't farming."))
                .setSaveConsumer(val -> funnyWaypoints = val)
                .build());

        farming.addEntry(entry.startBooleanToggle(Text.literal("Insta-Lane Switching"), instaSwitch)
                .setDefaultValue(false)
                .setTooltip(Text.literal("150ms when switching from right to left, useful in farming contests."))
                .setSaveConsumer(val -> instaSwitch = val)
                .build());

        farming.addEntry(entry.startBooleanToggle(Text.literal("Rewarp on pause"), rewarpOnPause)
                .setDefaultValue(false)
                .setTooltip(Text.literal("Sets a spawn point when you pause and warps you back when resumed."))
                .setSaveConsumer(val -> rewarpOnPause = val)
                .build());

        // Pests
        farming.addEntry(entry.startBooleanToggle(Text.literal("Pest Helper"), pestHelper)
                .setDefaultValue(false)
                .setTooltip(Text.literal("Draws a line and a box to the nearest pests."))
                .setSaveConsumer(val -> pestHelper = val)
                .build());

        farming.addEntry(entry.startBooleanToggle(Text.literal("Pest Farming mode"), pestFarming)
                .setDefaultValue(false)
                .setTooltip(Text.literal("Swaps equipment and pet when cooldown is over."))
                .setSaveConsumer(val -> pestFarming = val)
                .build());

        farming.addEntry(entry.startEnumSelector(Text.literal("Pet swap type"), PetSwapType.class, PetSwapType.values()[petSwapType])
                .setDefaultValue(PetSwapType.ROD)
                .setTooltip(Text.literal("Select a swapping method."))
                .setSaveConsumer(val -> petSwapType = val.ordinal())
                .build());

        farming.addEntry(
                entry.startIntField(Text.literal("Wardrobe slot"), 1)
                        .setDefaultValue(1)
                        .setTooltip(Text.literal("Type a number 1–9"))
                        .setSaveConsumer(val -> wardrobeSlot = MathHelper.clamp(val,1,9))
                        .build()
        );

        // Fishing Category
        ConfigCategory fishing = builder.getOrCreateCategory(Text.literal("Fishing"));

        fishing.addEntry(entry.startBooleanToggle(Text.literal("Fishing Helper"), fishingHelper)
                .setDefaultValue(false)
                .setTooltip(Text.literal("Enables the fishing helper."))
                .setSaveConsumer(val -> fishingHelper = val)
                .build());

        fishing.addEntry(entry.startBooleanToggle(Text.literal("Slug Trophy Fishing Helper"), slugFishing)
                .setDefaultValue(false)
                .setTooltip(Text.literal("Enables slug trophy fishing helper."))
                .setSaveConsumer(val -> slugFishing = val)
                .build());

        fishing.addEntry(entry.startBooleanToggle(Text.literal("Sea Creatures Killing Helper"), fishingHelperKilling)
                .setDefaultValue(false)
                .setTooltip(Text.literal("Enables sea creature killing, useful for lava fishing."))
                .setSaveConsumer(val -> fishingHelperKilling = val)
                .build());

        fishing.addEntry(entry.startIntSlider(Text.literal("Right Clicks Amount"), fishingHelperKillingAmount + 1, 1, 3)
                .setDefaultValue(1)
                .setTooltip(Text.literal("2 for hyp, 1 for veil, 1 or 2 for flay."))
                .setSaveConsumer(val -> fishingHelperKillingAmount = val - 1)
                .build());

        fishing.addEntry(entry.startStrField(Text.literal("Weapon"), fishingHelperKillingWeapon)
                .setDefaultValue("")
                .setTooltip(Text.literal("Write the weapon name to use for killing (hyperion, flay & veil all work)."))
                .setSaveConsumer(val -> fishingHelperKillingWeapon = val)
                .build());

        // Quality of Life Category
        ConfigCategory qol = builder.getOrCreateCategory(Text.literal("Quality of Life"));

        qol.addEntry(entry.startIntSlider(
                        Text.literal("Macro HUD X"),
                        macroHudX,
                        -500, 500
                )
                .setDefaultValue(0)
                .setTooltip(Text.literal("Horizontal offset of the macro HUD"))
                .setSaveConsumer(val -> macroHudX = val)
                .build());

        qol.addEntry(entry.startIntSlider(
                        Text.literal("Macro HUD Y"),
                        macroHudY,
                        -300, 300
                )
                .setDefaultValue(0)
                .setTooltip(Text.literal("Vertical offset of the macro HUD"))
                .setSaveConsumer(val -> macroHudY = val)
                .build());


        qol.addEntry(entry.startBooleanToggle(Text.literal("No Hurt Cam"), noHurtCam)
                .setDefaultValue(false)
                .setTooltip(Text.literal("Disable the hurt cam."))
                .setSaveConsumer(val -> noHurtCam = val)
                .build());

        qol.addEntry(entry.startBooleanToggle(Text.literal("Sphinx solver"), sphinxSolver)
                .setDefaultValue(false)
                .setTooltip(Text.literal("Auto answer sphinx questions."))
                .setSaveConsumer(val -> sphinxSolver = val)
                .build());

        qol.addEntry(entry.startBooleanToggle(Text.literal("Bigger Glass Panes Box"), fullBlockPanes)
                .setDefaultValue(false)
                .setTooltip(Text.literal("Increases the size of Glass Panes bounding box."))
                .setSaveConsumer(val -> fullBlockPanes = val)
                .build());

        // Keybinds Category
        ConfigCategory keybinds = builder.getOrCreateCategory(Text.literal("Keybinds"));

        keybinds.addEntry(entry.startKeyCodeField(Text.literal("Farming Macro Keybind"), toggleMacro.getDefaultKey())
                .setDefaultValue(InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_F8))
                .setKeySaveConsumer(toggleMacro::setBoundKey)
                .build());

        keybinds.addEntry(entry.startKeyCodeField(Text.literal("Pause & Unpause Farming Macro"), pauseMacro.getDefaultKey())
                .setDefaultValue(InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_P))
                .setKeySaveConsumer(pauseMacro::setBoundKey)
                .build());

        keybinds.addEntry(entry.startKeyCodeField(Text.literal("Reset Fake Fails"), resetFakeFails.getDefaultKey())
                .setDefaultValue(InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_BACKSPACE))
                .setKeySaveConsumer(resetFakeFails::setBoundKey)
                .build());

        // Debug
        ConfigCategory debugCat = builder.getOrCreateCategory(Text.literal("Debug"));
        debugCat.addEntry(entry.startBooleanToggle(Text.literal("Debug"), debug)
                .setDefaultValue(false)
                .setTooltip(Text.literal("Dev stuff."))
                .setSaveConsumer(val -> debug = val)
                .build());

        return builder.build();
    }

    public enum FarmType {
        MELON_PUMPKIN("Melon&Pumpkin -> MelonKingDe"),
        CANE_ROSE("Cane/Rose/Moon/Sun/Mushroom"),
        COCOA_BEANS("Cocoa Beans");

        private final String displayName;

        FarmType(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public enum PetSwapType {
        ROD("Fishing rod"),
        ARMOR("Wardrobe slot");

        private final String displayName;

        PetSwapType(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private static class ConfigData {
        boolean funnyToggle;
        int farmType;
        boolean funnyWaypoints;
        boolean instaSwitch;

        boolean pestHelper;
        boolean pestFarming;
        int petSwapType;
        int wardrobeSlot;
        boolean rewarpOnPause;
        boolean fishingHelper;
        boolean slugFishing;
        boolean fishingHelperKilling;
        int fishingHelperKillingAmount;
        String fishingHelperKillingWeapon;

        int macroHudX;
        int macroHudY;

        boolean noHurtCam;
        boolean sphinxSolver;
        boolean fullBlockPanes;

        boolean fishingHelperFireVeil;
        boolean fishingHelperFireVeilGalatea;
        boolean debug;

        ConfigData() {}

        ConfigData(
                boolean funnyToggle, int farmType, boolean funnyWaypoints, boolean instaSwitch,
                boolean pestHelper, boolean pestFarming, int petSwapType, int wardrobeSlot, boolean rewarpOnPause,
                boolean fishingHelper, boolean slugFishing, boolean fishingHelperKilling,
                int fishingHelperKillingAmount, String fishingHelperKillingWeapon,
                boolean noHurtCam, boolean sphinxSolver, boolean fullBlockPanes,
                boolean fishingHelperFireVeil, boolean fishingHelperFireVeilGalatea,
                boolean debug,
                int macroHudX, int macroHudY
        ) {
            this.funnyToggle = funnyToggle;
            this.farmType = farmType;
            this.funnyWaypoints = funnyWaypoints;
            this.instaSwitch = instaSwitch;

            this.pestHelper = pestHelper;
            this.rewarpOnPause = rewarpOnPause;
            this.pestFarming = pestFarming;
            this.petSwapType = petSwapType;
            this.wardrobeSlot = wardrobeSlot;
            this.fishingHelper = fishingHelper;
            this.slugFishing = slugFishing;
            this.fishingHelperKilling = fishingHelperKilling;
            this.fishingHelperKillingAmount = fishingHelperKillingAmount;
            this.fishingHelperKillingWeapon = fishingHelperKillingWeapon;

            this.noHurtCam = noHurtCam;
            this.sphinxSolver = sphinxSolver;
            this.fullBlockPanes = fullBlockPanes;

            this.fishingHelperFireVeil = fishingHelperFireVeil;
            this.fishingHelperFireVeilGalatea = fishingHelperFireVeilGalatea;
            this.debug = debug;

            this.macroHudX = macroHudX;
            this.macroHudY = macroHudY;
        }
    }
}