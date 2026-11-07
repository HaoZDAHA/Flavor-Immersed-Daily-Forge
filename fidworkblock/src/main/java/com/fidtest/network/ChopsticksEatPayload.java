package com.fidtest.network;

import com.fidtest.FidworkMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public record ChopsticksEatPayload(int slot, CompoundTag bite, ItemStack food) {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(FidworkMod.MODID, "chopsticks"), () -> "1", "1"::equals, "1"::equals);

    public static void register() {
        CHANNEL.messageBuilder(ChopsticksEatPayload.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((packet, buffer) -> {
                    buffer.writeVarInt(packet.slot());
                    buffer.writeNbt(packet.bite());
                    buffer.writeItem(packet.food());
                })
                .decoder(buffer -> new ChopsticksEatPayload(buffer.readVarInt(), buffer.readNbt(), buffer.readItem()))
                .consumerMainThread((packet, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                            com.fidtest.client.ChopsticksHandRenderer.startEating(packet.slot(), packet.bite(), packet.food()));
                    context.get().setPacketHandled(true);
                }).add();
    }

    public static void send(ServerPlayer player, ChopsticksEatPayload packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
