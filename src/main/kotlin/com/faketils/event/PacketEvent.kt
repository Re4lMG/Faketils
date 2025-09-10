package com.faketils.events

import net.minecraft.network.Packet
import net.minecraftforge.fml.common.eventhandler.Event

open class PacketEvent(val packet: Packet<*>) : Event() {

    class Send(packet: Packet<*>) : PacketEvent(packet)
    class Receive(packet: Packet<*>) : PacketEvent(packet)
}
