package com.faketils.mixin;

import com.faketils.events.PacketEvent;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public abstract class ClientConnectionMixin {

    @Inject(
            method = "send(Lnet/minecraft/network/protocol/Packet;)V",
            at = @At("HEAD")
    )
    private void faketils$onSend(Packet<?> packet, CallbackInfo ci) {
        PacketEvent.SEND.onSend(packet, (Connection) (Object) this);
    }

    @Inject(
            method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void faketils$onReceive(
            ChannelHandlerContext ctx,
            Packet<?> packet,
            CallbackInfo ci
    ) {
        PacketEvent.RECEIVE.onReceive(packet, (Connection) (Object) this);
    }
}