package com.faketils.commands

import com.faketils.Faketils
import net.minecraft.client.Minecraft
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import net.minecraft.util.BlockPos
import java.io.File
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class FarmingCommand : CommandBase() {

    private val mc: Minecraft = Minecraft.getMinecraft()

    companion object {
        val waypoints = mutableMapOf<String, MutableList<BlockPos>>()

        fun loadWaypoints() {
            val file = File(Faketils.configDirectory, "farming.json")
            if (file.exists()) {
                try {
                    val type = object : TypeToken<Map<String, List<Map<String, Int>>>>() {}.type
                    val data: Map<String, List<Map<String, Int>>> = Gson().fromJson(file.readText(), type)
                    waypoints.clear()
                    data.forEach { (key, list) ->
                        waypoints[key] = list.map { BlockPos(it["x"]!!, it["y"]!!, it["z"]!!) }.toMutableList()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        fun saveWaypoints() {
            try {
                val data = waypoints.mapValues { (_, list) ->
                    list.map { pos -> mapOf("x" to pos.x, "y" to pos.y, "z" to pos.z) }
                }
                val file = File(Faketils.configDirectory, "farming.json")
                file.writeText(Gson().toJson(data))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun getCommandName(): String = "ftset"

    override fun getCommandUsage(sender: ICommandSender): String = "/ftset <right|left|warp|reset>"

    override fun processCommand(sender: ICommandSender, args: Array<String>) {
        if (args.isEmpty()) {
            sender.addChatMessage(net.minecraft.util.ChatComponentText("§cUsage: /ftset <right|left|warp|reset>"))
            return
        }

        val player = mc.thePlayer ?: return
        val pos = BlockPos(player.posX, player.posY + 0.5, player.posZ)

        when (args[0].lowercase()) {
            "right" -> {
                val list = waypoints.getOrPut("right") { mutableListOf() }
                list.add(pos)
                sender.addChatMessage(net.minecraft.util.ChatComponentText("§aRight waypoint added!"))
            }
            "left" -> {
                val list = waypoints.getOrPut("left") { mutableListOf() }
                list.add(pos)
                sender.addChatMessage(net.minecraft.util.ChatComponentText("§cLeft waypoint added!"))
            }
            "reset" -> {
                waypoints.clear()
                saveWaypoints()
                sender.addChatMessage(net.minecraft.util.ChatComponentText("§eAll waypoints cleared!"))
                return
            }
            "warp" -> {
                val list = waypoints.getOrPut("warp") { mutableListOf() }
                list.add(pos)
                sender.addChatMessage(net.minecraft.util.ChatComponentText("§eWarp waypoint added!"))
            }
            else -> {
                sender.addChatMessage(net.minecraft.util.ChatComponentText("§cInvalid argument. Use right/left/reset"))
                return
            }
        }

        saveWaypoints()
    }

    override fun canCommandSenderUseCommand(sender: ICommandSender): Boolean = true
}