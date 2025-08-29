package com.faketils.config

import com.faketils.Faketils
import gg.essential.vigilance.Vigilant
import java.io.File

object Config : Vigilant(
    File(Faketils.configDirectory, "config.toml"),
    Faketils.metadata.name
) {
    var fullBlockLever = false

    var fullBlockButton = false

    var noHurtCam = false

    var fireFreezeTimer = false

    var macroStatus = false

    init {
        category("Quality of Life") {
            subcategory("Blocks") {
                switch(
                    ::fullBlockButton,
                    name = "Bigger button box",
                    description = "Increases the size of button bounding box."
                )
                switch(
                    ::fullBlockLever,
                    name = "Bigger lever box",
                    description = "Increases the size of lever bounding box."
                )
            }

            subcategory("Visual") {
                switch(
                    ::noHurtCam,
                    name = "No hurt cam",
                    description = "Disable the hurt cam."
                )
            }
        }
        category("Dungeons") {
            subcategory("M3/F3") {
                switch(
                    ::fireFreezeTimer,
                    name = "Fire freeze timer",
                    description = "Display the fire freeze timer in m3/f3."
                )
            }
        }
        category("Farming") {
            subcategory("Macro") {
                switch(
                    ::macroStatus,
                    name = "Macro status",
                    description = "Display the if the macro is active or not disable it if you aren't farming."
                )
            }
        }
    }
}