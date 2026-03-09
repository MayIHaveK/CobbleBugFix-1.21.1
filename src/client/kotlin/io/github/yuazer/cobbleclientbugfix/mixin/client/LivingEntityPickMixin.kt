package io.github.yuazer.cobbleclientbugfix.mixin.client

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(LivingEntity::class)
abstract class LivingEntityPickMixin {
    @Inject(
        method = ["method_5745"],
        at = [At("HEAD")],
        cancellable = true,
        require = 0,
        remap = false
    )
    private fun cobbleclientbugfix_guardPokemonPickNpeIntermediary(
        maxDistance: Double,
        partialTick: Float,
        includeFluids: Boolean,
        cir: CallbackInfoReturnable<HitResult>
    ) {
        cobbleclientbugfix_guardPick(maxDistance, partialTick, includeFluids, cir)
    }

    private fun cobbleclientbugfix_guardPick(
        maxDistance: Double,
        partialTick: Float,
        includeFluids: Boolean,
        cir: CallbackInfoReturnable<HitResult>
    ) {
        val self = this as LivingEntity
        if (self !is PokemonEntity) {
            return
        }

        val start = self.getEyePosition(partialTick)
        val view = self.getViewVector(partialTick)
        val end = start.add(view.x * maxDistance, view.y * maxDistance, view.z * maxDistance)
        val fluidMode = if (includeFluids) ClipContext.Fluid.ANY else ClipContext.Fluid.NONE
        cir.setReturnValue(
            self.level().clip(
                ClipContext(
                    start,
                    end,
                    ClipContext.Block.OUTLINE,
                    fluidMode,
                    self
                )
            )
        )
    }
}
