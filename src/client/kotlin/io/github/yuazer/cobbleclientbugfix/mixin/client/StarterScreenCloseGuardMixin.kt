package io.github.yuazer.cobbleclientbugfix.mixin.client

import com.cobblemon.mod.common.client.CobblemonClient
import com.cobblemon.mod.common.client.gui.startselection.StarterSelectionScreen
import io.github.yuazer.cobblebugfix.config.CobbleBugFixConfig
import io.github.yuazer.cobbleclientbugfix.client.StarterSelectionCloseBypassState
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Unique
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(Minecraft::class)
abstract class StarterScreenCloseGuardMixin {
    @Unique
    private var cobblebugfix_lastWarnMillis: Long = 0

    @Inject(method = ["method_1507(Lnet/minecraft/class_437;)V"], at = [At("HEAD")], cancellable = true)
    private fun cobblebugfix_preventClosingStarterScreen(next: Screen?, ci: CallbackInfo) {
        val client = this as Minecraft
        val current = client.screen
        if (next != null || current !is StarterSelectionScreen) {
            return
        }

        val playerData = CobblemonClient.clientPlayerData
        if (playerData.starterSelected) {
            return
        }

        val now = System.currentTimeMillis()
        if (StarterSelectionCloseBypassState.consumeIfRecent(now)) {
            return
        }
        if (now - cobblebugfix_lastWarnMillis > 1500) {
            cobblebugfix_lastWarnMillis = now
            client.player?.displayClientMessage(
                net.minecraft.network.chat.Component.literal(
                    CobbleBugFixConfig.getMessage(
                        "starterScreen.cannotClose",
                        "Please choose a starter before closing this screen."
                    )
                ),
                false
            )
        }
        ci.cancel()
    }
}
