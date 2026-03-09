package io.github.yuazer.cobbleclientbugfix.mixin.client

import com.cobblemon.mod.common.client.render.models.blockbench.PosableModel
import com.cobblemon.mod.common.client.render.models.blockbench.repository.RenderContext
import io.github.yuazer.cobbleclientbugfix.client.MissingModelPartReporter
import net.minecraft.client.model.geom.ModelPart
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(PosableModel::class)
abstract class PosableModelMixin {
    @Shadow
    abstract fun getRelevantPartsByName(): Map<String, ModelPart>

    @Shadow
    @JvmField
    var context: RenderContext? = null

    @Inject(method = ["getPart"], at = [At("HEAD")], cancellable = true)
    private fun cobbleclientbugfix_guardMissingPart(
        name: String?,
        cir: CallbackInfoReturnable<ModelPart>
    ) {
        if (name == null) {
            MissingModelPartReporter.report(this::class.java.name, null, context)
            cir.setReturnValue(MissingModelPartReporter.emptyModelPart())
            return
        }

        val parts = runCatching { getRelevantPartsByName() }.getOrNull()
        if (parts == null || !parts.containsKey(name)) {
            MissingModelPartReporter.report(this::class.java.name, name, context)
            cir.setReturnValue(MissingModelPartReporter.emptyModelPart())
        }
    }
}
