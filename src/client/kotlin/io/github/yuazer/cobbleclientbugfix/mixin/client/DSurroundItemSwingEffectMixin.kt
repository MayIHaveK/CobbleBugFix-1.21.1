package io.github.yuazer.cobbleclientbugfix.mixin.client

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Pseudo
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Redirect

@Pseudo
@Mixin(targets = ["org.orecruncher.dsurround.effects.entity.ItemSwingEffect"], remap = false)
abstract class DSurroundItemSwingEffectMixin {
    @Redirect(
        method = ["rayTraceBlock"],
        at = At(
            value = "INVOKE",
            target = "Lnet/minecraft/class_1309;method_5745(DFZ)Lnet/minecraft/class_239;",
            remap = false
        ),
        require = 0,
        remap = false
    )
    private fun cobbleclientbugfix_guardPickCall(
        entity: LivingEntity,
        maxDistance: Double,
        partialTick: Float,
        includeFluids: Boolean
    ): HitResult {
        return try {
            entity.pick(maxDistance, partialTick, includeFluids)
        } catch (npe: NullPointerException) {
            if (entity !is PokemonEntity || !isLocatorNullNpe(npe)) {
                throw npe
            }
            fallbackPick(entity, maxDistance, partialTick, includeFluids)
        }
    }

    private fun fallbackPick(
        entity: LivingEntity,
        maxDistance: Double,
        partialTick: Float,
        includeFluids: Boolean
    ): HitResult {
        val start = entity.getEyePosition(partialTick)
        val view = entity.getViewVector(partialTick)
        val end = start.add(view.x * maxDistance, view.y * maxDistance, view.z * maxDistance)
        val fluidMode = if (includeFluids) ClipContext.Fluid.ANY else ClipContext.Fluid.NONE
        return entity.level().clip(
            ClipContext(
                start,
                end,
                ClipContext.Block.OUTLINE,
                fluidMode,
                entity
            )
        )
    }

    private fun isLocatorNullNpe(npe: NullPointerException): Boolean {
        val message = npe.message ?: return false
        return message.contains("locator", ignoreCase = true)
    }
}
