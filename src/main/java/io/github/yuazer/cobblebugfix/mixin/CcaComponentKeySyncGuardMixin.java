package io.github.yuazer.cobblebugfix.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * On hybrid servers (Arclight), channel negotiation can be inconsistent.
 * For CCA entity sync packets, skip forced disconnect and just drop sync.
 */
@Mixin(targets = "org.ladysnake.cca.api.v3.component.ComponentKey")
public abstract class CcaComponentKeySyncGuardMixin {

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("CobbleBugFix");

    @Redirect(
            method = "syncWith(Lnet/minecraft/server/level/ServerPlayer;Lorg/ladysnake/cca/api/v3/component/ComponentProvider;Lorg/ladysnake/cca/api/v3/component/sync/ComponentPacketWriter;Lorg/ladysnake/cca/api/v3/component/sync/PlayerSyncPredicate;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;disconnect(Lnet/minecraft/network/chat/Component;)V"
            )
    )
    private void cobble$skipCcaEntitySyncKick(ServerGamePacketListenerImpl connection, Component reason) {
        String message = reason == null ? "" : reason.getString();

        if (message.contains("cardinal-components:entity_sync")) {
            LOGGER.info("[CobbleBugFix] Skipped CCA forced disconnect (reason: {})", message);
            return;
        }

        connection.disconnect(reason);
    }
}
