package com.faketils.config;

import dev.isxander.yacl3.config.v2.api.*;
import dev.isxander.yacl3.config.v2.api.autogen.*;
import dev.isxander.yacl3.config.v2.api.autogen.Boolean;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;

import org.lwjgl.glfw.GLFW;

public class Config {

    public static final ConfigClassHandler<Config> HANDLER =
            ConfigClassHandler.createBuilder(Config.class)
                    .id(Identifier.of("faketils", "config"))
                    .serializer(config -> GsonConfigSerializerBuilder.create(config)
                            .setPath(FabricLoader.getInstance().getConfigDir().resolve("faketils.json"))
                            .setJson5(false)
                            .build())
                    .build();

    public static final KeyBinding.Category FAKETILS_CATEGORY =
            new KeyBinding.Category(Identifier.of("faketils"));

    public static final KeyBinding toggleMacro =
            KeyBindingHelper.registerKeyBinding(
                    new KeyBinding("key.faketils.toggle_macro",
                            InputUtil.Type.KEYSYM,
                            GLFW.GLFW_KEY_F8,
                            FAKETILS_CATEGORY)
            );

    public static final KeyBinding pauseMacro =
            KeyBindingHelper.registerKeyBinding(
                    new KeyBinding("key.faketils.pause_macro",
                            InputUtil.Type.KEYSYM,
                            GLFW.GLFW_KEY_P,
                            FAKETILS_CATEGORY)
            );

    public static final KeyBinding resetFakeFails =
            KeyBindingHelper.registerKeyBinding(
                    new KeyBinding("key.faketils.reset_fails",
                            InputUtil.Type.KEYSYM,
                            GLFW.GLFW_KEY_BACKSPACE,
                            FAKETILS_CATEGORY)
            );

    @AutoGen(category = "farming", group = "general")
    @Boolean
    @SerialEntry
    @CustomName("Funny Toggle")
    @CustomDescription("Display if the funny is active or not. Disable if you aren't farming.")
    public boolean funnyToggle = false;

    @AutoGen(category = "farming", group = "general")
    @EnumCycler
    @SerialEntry
    @CustomName("Farm Type")
    @CustomDescription("Select a farm.")
    public FarmType farmType = FarmType.MELON_PUMPKIN;

    @AutoGen(category = "farming", group = "general")
    @Boolean
    @SerialEntry
    @CustomName("Show Waypoints")
    @CustomDescription("Display the lane switching waypoints. Disable if you aren't farming.")
    public boolean funnyWaypoints = false;

    @AutoGen(category = "farming", group = "general")
    @Boolean
    @SerialEntry
    @CustomName("Insta-Lane Switching")
    @CustomDescription("150ms when switching from right to left, useful in farming contests.")
    public boolean instaSwitch = false;

    @AutoGen(category = "farming", group = "general")
    @Boolean
    @SerialEntry
    @CustomName("Auto Spraynator")
    @CustomDescription("Automatically sprays the plot you are currently farming in.")
    public boolean autoSpray = false;

    @AutoGen(category = "farming", group = "general")
    @Boolean
    //@SerialEntry
    @CustomName("Auto Sell Farming Junk (bugged)")
    @CustomDescription("Automatically sells vinyls and overclockers. Requires a Booster Cookie.")
    public boolean autoSellJunk = false;

    @AutoGen(category = "farming", group = "general")
    @Boolean
    @SerialEntry
    @CustomName("Rewarp On Pause")
    @CustomDescription("Sets a spawn point when the macro is paused and warps back when resumed.")
    public boolean rewarpOnPause = false;

    @AutoGen(category = "farming", group = "pests")
    @Boolean
    @SerialEntry
    @CustomName("Pest Helper")
    @CustomDescription("Draws a line and box to the nearest pest.")
    public boolean pestHelper = false;

    @AutoGen(category = "farming", group = "pests")
    @Boolean
    @SerialEntry
    @CustomName("Pest Farming Mode")
    @CustomDescription("Automatically swaps equipment and pet when the pest cooldown is over.")
    public boolean pestFarming = false;

    @AutoGen(category = "farming", group = "pests")
    @Boolean
    @SerialEntry
    @CustomName("Auto Pest Killing")
    @CustomDescription("Automatically kills pests (risky).")
    public boolean pestKilling = false;

    @AutoGen(category = "farming", group = "pests")
    @IntSlider(min = 75, max = 300, step = 1)
    @SerialEntry
    @CustomName("Pest Cooldown Time")
    @CustomDescription("Cooldown time for pests in seconds.")
    public int pestTime = 135;

    @AutoGen(category = "farming", group = "pet_swapping")
    @EnumCycler
    @SerialEntry
    @CustomName("Pet Swap Type")
    @CustomDescription("Select the method used to swap pets.")
    public PetSwapType petSwapType = PetSwapType.ROD;

    @AutoGen(category = "farming", group = "pet_swapping")
    @IntField(min = 20, max = 1500)
    @SerialEntry
    @CustomName("Equipment Click Delay")
    @CustomDescription("Delay between equipment clicks in milliseconds.")
    public int swapDelay = 150;

    @AutoGen(category = "farming", group = "pet_swapping")
    @IntField(min = 1, max = 9)
    @SerialEntry
    @CustomName("Mossy Wardrobe Slot")
    @CustomDescription("Wardrobe slot used for Mossy equipment.")
    public int wardrobeSlot = 1;

    @AutoGen(category = "farming", group = "pet_swapping")
    @IntField(min = 1, max = 9)
    @SerialEntry
    @CustomName("Mantid Wardrobe Slot")
    @CustomDescription("Wardrobe slot used for Mantid equipment.")
    public int wardrobeSlotOld = 1;

    @AutoGen(category = "fishing", group = "helpers")
    @Boolean
    @SerialEntry
    @CustomName("Fishing Helper")
    @CustomDescription("Enables the fishing helper.")
    public boolean fishingHelper = false;

    @AutoGen(category = "fishing", group = "helpers")
    @Boolean
    @SerialEntry
    @CustomName("Slug Trophy Fishing")
    @CustomDescription("Enables the slug trophy fishing helper.")
    public boolean slugFishing = false;

    @AutoGen(category = "fishing", group = "helpers")
    @Boolean
    @SerialEntry
    @CustomName("Sea Creature Killing")
    @CustomDescription("Automatically helps kill sea creatures (useful for lava fishing).")
    public boolean fishingHelperKilling = false;

    @AutoGen(category = "fishing", group = "helpers")
    @IntSlider(min = 1, max = 3, step = 1)
    @SerialEntry
    @CustomName("Right Click Amount")
    @CustomDescription("Number of right clicks used to kill sea creatures.")
    public int fishingHelperKillingAmount = 1;

    @AutoGen(category = "fishing", group = "helpers")
    @StringField
    @SerialEntry
    @CustomName("Weapon Name")
    @CustomDescription("Name of the weapon used for killing sea creatures (Hyperion, Flay, Veil, etc).")
    public String fishingHelperKillingWeapon = "";

    @AutoGen(category = "qol", group = "hud")
    @IntSlider(min = -500, max = 500, step = 1)
    @SerialEntry
    @CustomName("Macro HUD X")
    @CustomDescription("Horizontal position offset of the macro HUD.")
    public int macroHudX = 60;

    @AutoGen(category = "qol", group = "hud")
    @IntSlider(min = -300, max = 300, step = 1)
    @SerialEntry
    @CustomName("Macro HUD Y")
    @CustomDescription("Vertical position offset of the macro HUD.")
    public int macroHudY = 200;

    @AutoGen(category = "qol", group = "other")
    @Boolean
    @SerialEntry
    @CustomName("No Hurt Cam")
    @CustomDescription("Disables the hurt camera shake effect.")
    public boolean noHurtCam = false;

    @AutoGen(category = "qol", group = "other")
    @Boolean
    @SerialEntry
    @CustomName("Sphinx Solver")
    @CustomDescription("Automatically answers Sphinx riddles.")
    public boolean sphinxSolver = false;

    @AutoGen(category = "qol", group = "other")
    @Boolean
    @SerialEntry
    @CustomName("Full Block Panes")
    @CustomDescription("Increases the bounding box size of glass panes.")
    public boolean fullBlockPanes = false;

    @AutoGen(category = "debug", group = "dev")
    @Boolean
    @SerialEntry
    @CustomName("Debug Mode")
    @CustomDescription("Enables developer debug features.")
    public boolean debug = false;

    public static Screen createScreen(Screen parent) {
        return HANDLER.generateGui().generateScreen(parent);
    }

    public enum FarmType {
        MELON_PUMPKIN,
        CANE_ROSE,
        COCOA_BEANS
    }

    public enum PetSwapType {
        ROD,
        ARMOR
    }
}