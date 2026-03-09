package io.github.yuazer.cobbleclientbugfix.mixin.client;

import com.cobblemon.mod.common.CobblemonNetwork;
import com.cobblemon.mod.common.api.net.NetworkPacket;
import com.cobblemon.mod.common.net.messages.server.SelectStarterPacket;
import io.github.yuazer.cobbleclientbugfix.client.StarterSelectionCloseBypassState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CobblemonNetwork.class)
public abstract class CobblemonNetworkStarterSelectFlagMixin {
    @Inject(
        method = "sendToServer(Lcom/cobblemon/mod/common/api/net/NetworkPacket;)V",
        at = @At("HEAD"),
        remap = false
    )
    private static void cobblebugfix_markStarterSelectionAttempt(NetworkPacket<?> packet, CallbackInfo ci) {
        if (packet instanceof SelectStarterPacket) {
            StarterSelectionCloseBypassState.markSelectStarterSent();
        }
    }
}
