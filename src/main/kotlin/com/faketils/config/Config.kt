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
    }
}