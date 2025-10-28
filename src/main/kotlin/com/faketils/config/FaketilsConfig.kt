package com.faketils.config

import cc.polyfrost.oneconfig.config.Config
import cc.polyfrost.oneconfig.config.annotations.HUD
import cc.polyfrost.oneconfig.config.annotations.KeyBind
import cc.polyfrost.oneconfig.config.annotations.Slider
import cc.polyfrost.oneconfig.config.annotations.Switch
import cc.polyfrost.oneconfig.config.annotations.Text
import cc.polyfrost.oneconfig.config.core.OneKeyBind
import cc.polyfrost.oneconfig.config.data.Mod
import cc.polyfrost.oneconfig.config.data.ModType
import com.faketils.hud.InvSpecHUD
import com.faketils.hud.MacroStatusHUD
import org.lwjgl.input.Keyboard

object FaketilsConfig : Config(
    Mod("Faketils", ModType.UTIL_QOL),
    "faketils.json"
) {
    @Switch(
        name = "Bigger Button Box",
        description = "Increases the size of button bounding box.",
        category = "Quality of Life",
        subcategory = "Blocks"
    )
    var fullBlockButton = false

    @Switch(
        name = "Bigger Lever Box",
        description = "Increases the size of lever bounding box.",
        category = "Quality of Life",
        subcategory = "Blocks"
    )
    var fullBlockLever = false

    @Switch(
        name = "Bigger Glass Panes Box",
        description = "Increases the size of Glass Panes bounding box.",
        category = "Quality of Life",
        subcategory = "Blocks"
    )
    var fullBlockPanes = false

    @Switch(
        name = "No Hurt Cam",
        description = "Disable the hurt cam.",
        category = "Quality of Life",
        subcategory = "Visual"
    )
    var noHurtCam = false

    @Switch(
        name = "Hide Dying Mobs",
        description = "Hide death animation of the entities.",
        category = "Quality of Life",
        subcategory = "Visual"
    )
    var hideDyingMobs = false

    @Switch(
        name = "Performance mode",
        description = "Toggle performance mode.",
        category = "Quality of Life",
        subcategory = "Performance"
    )
    var performanceMode = false

    @Slider(
        name = "Larger Head Scale",
        description = "Scale the heads inside the inventory.",
        category = "Quality of Life",
        subcategory = "Visual",
        min = 0.1f,
        max = 3.0f
    )
    var largerHeadScale = 1.0f

    @Slider(
        name = "Item rarity display",
        description = "Changes the opacity of the background rarity of an item, set to 0 to disable.",
        category = "Quality of Life",
        subcategory = "Visual",
        min = 0.0f,
        max = 1.0f
    )
    var itemRarity = 0.0f

    @Slider(
        name = "Dropped Item Size",
        description = "Change the size of dropped items.",
        category = "Quality of Life",
        subcategory = "Visual",
        min = 0.1f,
        max = 3.0f
    )
    var itemDropScale = 1.0f

    @Switch(
        name = "Fire Freeze Timer",
        description = "Display the fire freeze timer in m3/f3.",
        category = "Dungeons",
        subcategory = "M3/F3"
    )
    var fireFreezeTimer = false

    @Switch(
        name = "Funny Toggle",
        description = "Display if the funny is active or not. Disable if you aren't farming.",
        category = "Farming",
        subcategory = "Funny"
    )
    var funnyToggle = false

    @Switch(
        name = "Show waypoints",
        description = "Display the lane switching waypoints. Disable if you aren't farming.",
        category = "Farming",
        subcategory = "Funny"
    )
    var funnyWaypoints = false

    @KeyBind(
        name = "Macro keybind",
        category = "Keybinds",
        subcategory = "Macro",
        description = "Toggles the macro on/off",
        size = 2
    )
    var toggleMacro = OneKeyBind(Keyboard.KEY_F8)

    @KeyBind(
        name = "Pause & Unpause Macro",
        category = "Keybinds",
        subcategory = "Macro",
        description = "Pauses or unpauses the macro",
        size = 2
    )
    var pauseMacro = OneKeyBind(Keyboard.KEY_P)

    @KeyBind(
        name = "Reset Fake Fails",
        category = "Keybinds",
        subcategory = "Macro",
        description = "Resets fake fails for the macro",
        size = 2
    )
    var resetFakeFails = OneKeyBind(Keyboard.KEY_BACK)

    @Switch(
        name = "Pest Helper",
        description = "Draws a line and a box to the nearest pests.",
        category = "Farming",
        subcategory = "Pests"
    )
    var pestHelper = false

    @Switch(
        name = "Fishing Helper",
        description = "Enables the fishing helper.",
        category = "Fishing",
        subcategory = "Funny"
    )
    var fishingHelper = false

    @Switch(
        name = "Slug Trophy Fishing Helper",
        description = "Enables slug trophy fishing helper.",
        category = "Fishing",
        subcategory = "Funny"
    )
    var slugFishing = false

    @Switch(
        name = "Sea Creatures Killing Helper",
        description = "Enables sea creature killing, useful for lava fishing.",
        category = "Fishing",
        subcategory = "Funny"
    )
    var fishingHelperKilling = false

    @Text(
        name = "Weapon",
        description = "Write the weapon name to use for killing (hyperion, flay & veil all work).",
        category = "Fishing",
        subcategory = "Funny",
        placeholder = "Enter weapon name"
    )
    var fishingHelperKillingWeapon = ""

    @Switch(
        name = "Debug",
        description = "Dev stuff.",
        category = "Debug",
        subcategory = "Debug"
    )
    var debug = false

    @HUD(
        name = "Farming Macro status HUD",
        category = "HUD",
        subcategory = "Farming"
    )
    var macroStatusHUD = MacroStatusHUD()

    @HUD(
         name = "Inventory HUD",
         category = "HUD",
         subcategory = "Misc"
    )
    var invSpecHUD = InvSpecHUD()

    init {
        initialize()
    }
}