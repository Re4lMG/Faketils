package com.faketils.config

import com.faketils.Faketils
import gg.essential.vigilance.Vigilant
import gg.essential.vigilance.data.Property
import gg.essential.vigilance.data.PropertyType
import java.io.File

object Config : Vigilant(
    File(Faketils.configDirectory, "config.toml"),
    Faketils.metadata.name
) {

    @Property(
        type = PropertyType.SWITCH, name = "Bigger Button Box",
        description = "Increases the size of button bounding box.",
        category = "Quality of Life", subcategory = "Blocks"
    )
    var fullBlockButton = false

    @Property(
        type = PropertyType.SWITCH, name = "Bigger Lever Box",
        description = "Increases the size of lever bounding box.",
        category = "Quality of Life", subcategory = "Blocks"
    )
    var fullBlockLever = false

    @Property(
        type = PropertyType.SWITCH, name = "Bigger Glass Panes Box",
        description = "Increases the size of Glass Panes bounding box.",
        category = "Quality of Life", subcategory = "Blocks"
    )
    var fullBlockPanes = false

    @Property(
        type = PropertyType.SWITCH, name = "No Hurt Cam",
        description = "Disable the hurt cam.",
        category = "Quality of Life", subcategory = "Visual"
    )
    var noHurtCam = false

    @Property(
        type = PropertyType.SWITCH, name = "Hide Dying Mobs",
        description = "Hide death animation of the entities.",
        category = "Quality of Life", subcategory = "Visual"
    )
    var hideDyingMobs = false

    @Property(
        type = PropertyType.SWITCH, name = "Performance mode",
        description = "Toggle performance mode.",
        category = "Quality of Life", subcategory = "Performance"
    )
    var performanceMode = false

    @Property(
        type = PropertyType.DECIMAL_SLIDER, name = "Larger Head Scale",
        description = "Scale the heads inside the inventory.",
        category = "Quality of Life", subcategory = "Visual",
        maxF = 3f, minF = 0.1f, decimalPlaces = 2
    )
    var largerHeadScale = 1f

    @Property(
        type = PropertyType.PERCENT_SLIDER, name = "Item rarity display",
        description = "Changes the opacity of the background rarity of an item, set to 0 to disable.",
        category = "Quality of Life", subcategory = "Visual",
    )
    var itemRarity = 0f

    @Property(
        type = PropertyType.DECIMAL_SLIDER, name = "Dropped Item Size",
        description = "Change the size of dropped items.",
        category = "Quality of Life", subcategory = "Visual",
        maxF = 3f, minF = 0.1f, decimalPlaces = 2
    )
    var itemDropScale = 1f

    @Property(
        type = PropertyType.SWITCH, name = "Fire Freeze Timer",
        description = "Display the fire freeze timer in m3/f3.",
        category = "Dungeons", subcategory = "M3/F3"
    )
    var fireFreezeTimer = false

    @Property(
        type = PropertyType.SWITCH, name = "Funny Toggle",
        description = "Display if the funny is active or not. Disable if you aren't farming.",
        category = "Farming", subcategory = "Funny"
    )
    var funnyToggle = false

    @Property(
        type = PropertyType.SWITCH, name = "Fishing Helper",
        description = "Enables the fishing helper.",
        category = "Fishing", subcategory = "Funny"
    )
    var fishingHelper = false

    @Property(
        type = PropertyType.SWITCH, name = "Fire Veil killing",
        description = "Enables the fire veil killing, useful for lava fishing.",
        category = "Fishing", subcategory = "Funny"
    )
    var fishingHelperFireVeil = false

    @Property(
        type = PropertyType.SWITCH, name = "Debug",
        description = "Dev stuff.",
        category = "Debug", subcategory = "Debug"
    )
    var debug = false
}
