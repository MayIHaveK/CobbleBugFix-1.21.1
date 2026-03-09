package io.github.yuazer.cobblebugfix.mixin;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import io.github.yuazer.cobblebugfix.config.CobbleBugFixConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PokemonEntity.class)
public class PokemonEntityOfferHeldItemMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger("CobbleBugFix");

    @Inject(
            method = "offerHeldItem(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cobble$denyOfferHeldItemWhileEvolving(
            Player player,
            ItemStack stack,
            CallbackInfoReturnable<Boolean> cir
    ) {
        PokemonEntity self = (PokemonEntity) (Object) this;
        boolean evolvingOrChangingForm =
                self.isEvolving()
                        || self.getPokemon().getPersistentData().getBoolean("form_changing");

        if (evolvingOrChangingForm) {
            if (player instanceof ServerPlayer sp) {
                sp.sendSystemMessage(Component.literal(
                        CobbleBugFixConfig.getMessage(
                                "denyHeldItemWhileEvolving",
                                "§c当前宝可梦正在进化，无法给予携带物！"
                        )
                ));
            }
            cir.setReturnValue(false);
        }
    }
}
