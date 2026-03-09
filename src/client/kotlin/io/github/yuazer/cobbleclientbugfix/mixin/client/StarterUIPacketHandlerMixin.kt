package io.github.yuazer.cobbleclientbugfix.mixin.client

import com.cobblemon.mod.common.client.CobblemonClient
import com.cobblemon.mod.common.client.gui.startselection.StarterSelectionScreen
import com.cobblemon.mod.common.client.net.starter.StarterUIPacketHandler
import com.cobblemon.mod.common.net.messages.client.starter.OpenStarterUIPacket
import net.minecraft.client.Minecraft
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(StarterUIPacketHandler::class)
abstract class StarterUIPacketHandlerMixin {
    @Inject(
        method = ["handle(Lcom/cobblemon/mod/common/net/messages/client/starter/OpenStarterUIPacket;Lnet/minecraft/class_310;)V"],
        at = [At("HEAD")],
        cancellable = true
    )
    private fun cobblebugfix_skipDuplicateStarterScreenOpen(
        packet: OpenStarterUIPacket,
        client: Minecraft,
        ci: CallbackInfo
    ) {
        if (client.screen !is StarterSelectionScreen) {
            return
        }
        if (CobblemonClient.clientPlayerData.starterSelected) {
            return
        }
        ci.cancel()
    }
}
