package io.github.yuazer.cobblebugfix.mixin;

import com.cobblemon.mod.common.client.gui.interact.wheel.InteractTypePokemon;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.net.messages.server.pokemon.interact.InteractPokemonPacket;
import com.cobblemon.mod.common.net.serverhandling.pokemon.interact.InteractPokemonHandler;
import io.github.yuazer.cobblebugfix.config.CobbleBugFixConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InteractPokemonHandler.class)
public class InteractPokemonHandlerMixin {

    @Inject(
            method = "handle(Lcom/cobblemon/mod/common/net/messages/server/pokemon/interact/InteractPokemonPacket;Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cobble$denyHeldItemWhileEvolving(
            InteractPokemonPacket packet,
            MinecraftServer server,
            ServerPlayer player,
            CallbackInfo ci
    ) {
        if (packet.getInteractType() != InteractTypePokemon.HELD_ITEM) {
            return;
        }

        Entity entity = player.serverLevel().getEntity(packet.getPokemonID());
        if (!(entity instanceof PokemonEntity pokemonEntity)) {
            return;
        }
        if (pokemonEntity.isBattleClone()) {
            return;
        }
        boolean evolvingOrChangingForm =
                pokemonEntity.isEvolving()
                        || pokemonEntity.getPokemon().getPersistentData().getBoolean("form_changing");

        if (evolvingOrChangingForm) {
            if (player instanceof ServerPlayer sp) {
                sp.sendSystemMessage(Component.literal(
                        CobbleBugFixConfig.getMessage(
                                "denyHeldItemWhileEvolving",
                                "§c当前宝可梦正在进化，无法给予携带物！"
                        )
                ));
            }
            ci.cancel();
        }
    }
}
