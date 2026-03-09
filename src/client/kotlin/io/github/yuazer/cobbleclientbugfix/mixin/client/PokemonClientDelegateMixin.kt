package io.github.yuazer.cobbleclientbugfix.mixin.client

import com.cobblemon.mod.common.client.entity.PokemonClientDelegate
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import io.github.yuazer.cobbleclientbugfix.client.SeatRideReporter
import net.minecraft.world.entity.Entity
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(PokemonClientDelegate::class)
abstract class PokemonClientDelegateMixin {
    @Shadow
    abstract fun getEntity(): PokemonEntity

    @Inject(method = ["getSeatLocator"], at = [At("HEAD")], cancellable = true)
    private fun cobbleclientbugfix_guardSeatLocator(
        passenger: Entity?,
        cir: CallbackInfoReturnable<String>
    ) {
        if (passenger == null) {
            return
        }
        val entity = getEntity()
        val occupied = entity.occupiedSeats
        if (occupied == null || occupied.none { it === passenger }) {
            SeatRideReporter.report(entity, passenger.name.string)
            cir.setReturnValue("seat_1")
        }
    }
}
