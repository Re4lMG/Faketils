package com.faketils.events;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;

public class PacketEvent {

    public interface Send {
        void onSend(Packet<?> packet, Connection connection);
    }

    public interface Receive {
        void onReceive(Packet<?> packet, Connection connection);
    }

    public static Send SEND = (packet, connection) -> {};
    public static Receive RECEIVE = (packet, connection) -> {};

    public static void registerSend(Send listener) {
        Send old = SEND;
        SEND = (packet, connection) -> {
            old.onSend(packet, connection);
            listener.onSend(packet, connection);
        };
    }

    public static void registerReceive(Receive listener) {
        Receive old = RECEIVE;
        RECEIVE = (packet, connection) -> {
            old.onReceive(packet, connection);
            listener.onReceive(packet, connection);
        };
    }
}