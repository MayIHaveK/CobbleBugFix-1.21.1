package io.github.yuazer.cobblebugfix.mixin;

import com.cobblemon.mod.common.net.messages.client.storage.pc.OpenPCPacket;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


import static com.cobblemon.mod.common.util.PlayerExtensionsKt.party;

@Mixin(OpenPCPacket.class)
public abstract class OpenPCPacketMixin {

    @Inject(method = "sendToPlayer", at = @At("TAIL"))
    private void afterSendToPlayer(ServerPlayer player, CallbackInfo ci) {
        // 只在 OpenPCPacket#sendToPlayer 后执行
        party(player).swap(1, 1);
    }
}
