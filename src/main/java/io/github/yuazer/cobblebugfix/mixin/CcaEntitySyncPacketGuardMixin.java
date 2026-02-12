package io.github.yuazer.cobblebugfix.mixin;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevent sending CCA entity_sync payload to clients that can't receive it,
 * avoiding disconnect: "unhandled packet: cardinal-components:entity_sync".
 */
@Mixin(ServerPlayNetworking.class)
public abstract class CcaEntitySyncPacketGuardMixin {

    private static final ResourceLocation CCA_ENTITY_SYNC = new ResourceLocation("cardinal-components", "entity_sync");

    @Inject(
            method = "send(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void cobble$guardCcaEntitySync(ServerPlayer player, CustomPacketPayload payload, CallbackInfo ci) {
        if (player == null || payload == null) return;

        // payload.type().id() is the channel ResourceLocation
        ResourceLocation id = payload.type().id();

        if (CCA_ENTITY_SYNC.equals(id) && !ServerPlayNetworking.canSend(player, CCA_ENTITY_SYNC)) {
            // Skip sending to avoid client disconnect
            ci.cancel();
        }
    }
}
