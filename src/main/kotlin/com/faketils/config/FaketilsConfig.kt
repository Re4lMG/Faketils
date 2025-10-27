package com.faketils.config

import cc.polyfrost.oneconfig.config.Config
import cc.polyfrost.oneconfig.config.annotations.Slider
import cc.polyfrost.oneconfig.config.annotations.Switch
import cc.polyfrost.oneconfig.config.annotations.Text
import cc.polyfrost.oneconfig.config.data.Mod
import cc.polyfrost.oneconfig.config.data.ModType

class FaketilsConfig : Config(
    Mod("Faketils", ModType.UTIL_QOL),
    "faketils.json"
) {
    @Switch(
        name = "Bigger Button Box",
        description = "Increases the size of button bounding box.",
        category = "Quality of Life",
        subcategory = "Blocks"
    )
    var fullBlockButton: Boolean = false

    @Switch(
        name = "Bigger Lever Box",
        description = "Increases the size of lever bounding box.",
        category = "Quality of Life",
        subcategory = "Blocks"
    )
    var fullBlockLever: Boolean = false

    @Switch(
        name = "Bigger Glass Panes Box",
        description = "Increases the size of Glass Panes bounding box.",
        category = "Quality of Life",
        subcategory = "Blocks"
    )
    var fullBlockPanes: Boolean = false

    @Switch(
        name = "No Hurt Cam",
        description = "Disable the hurt cam.",
        category = "Quality of Life",
        subcategory = "Visual"
    )
    var noHurtCam: Boolean = false

    @Switch(
        name = "Hide Dying Mobs",
        description = "Hide death animation of the entities.",
        category = "Quality of Life",
        subcategory = "Visual"
    )
    var hideDyingMobs: Boolean = false

    @Switch(
        name = "Performance mode",
        description = "Toggle performance mode.",
        category = "Quality of Life",
        subcategory = "Performance"
    )
    var performanceMode: Boolean = false

    @Slider(
        name = "Larger Head Scale",
        description = "Scale the heads inside the inventory.",
        category = "Quality of Life",
        subcategory = "Visual",
        min = 0.1f,
        max = 3.0f
    )
    var largerHeadScale: Float = 1.0f

    @Slider(
        name = "Item rarity display",
        description = "Changes the opacity of the background rarity of an item, set to 0 to disable.",
        category = "Quality of Life",
        subcategory = "Visual",
        min = 0.0f,
        max = 1.0f
    )
    var itemRarity: Float = 0.0f

    @Slider(
        name = "Dropped Item Size",
        description = "Change the size of dropped items.",
        category = "Quality of Life",
        subcategory = "Visual",
        min = 0.1f,
        max = 3.0f
    )
    var itemDropScale: Float = 1.0f

    @Switch(
        name = "Fire Freeze Timer",
        description = "Display the fire freeze timer in m3/f3.",
        category = "Dungeons",
        subcategory = "M3/F3"
    )
    var fireFreezeTimer: Boolean = false

    @Switch(
        name = "Funny Toggle",
        description = "Display if the funny is active or not. Disable if you aren't farming.",
        category = "Farming",
        subcategory = "Funny"
    )
    var funnyToggle: Boolean = false

    @Switch(
        name = "Pest Helper",
        description = "Draws a line and a box to the nearest pests.",
        category = "Farming",
        subcategory = "Pests"
    )
    var pestHelper: Boolean = false

    @Switch(
        name = "Fishing Helper",
        description = "Enables the fishing helper.",
        category = "Fishing",
        subcategory = "Funny"
    )
    var fishingHelper: Boolean = false

    @Switch(
        name = "Slug Trophy Fishing Helper",
        description = "Enables slug trophy fishing helper.",
        category = "Fishing",
        subcategory = "Funny"
    )
    var slugFishing: Boolean = false

    @Switch(
        name = "Sea Creatures Killing Helper",
        description = "Enables sea creature killing, useful for lava fishing.",
        category = "Fishing",
        subcategory = "Funny"
    )
    var fishingHelperKilling: Boolean = false

    @Text(
        name = "Weapon",
        description = "Write the weapon name to use for killing (hyperion, flay & veil all work).",
        category = "Fishing",
        subcategory = "Funny",
        placeholder = "Enter weapon name"
    )
    var fishingHelperKillingWeapon: String = ""

    @Switch(
        name = "Debug",
        description = "Dev stuff.",
        category = "Debug",
        subcategory = "Debug"
    )
    var debug: Boolean = false

    init {
        initialize()
    }
}