package io.github.yuazer.cobbleclientbugfix.client

import com.cobblemon.mod.common.client.render.models.blockbench.repository.RenderContext
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import io.github.yuazer.cobblebugfix.config.CobbleBugFixConfig
import net.minecraft.client.Minecraft
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.network.chat.Component
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

object MissingModelPartReporter {
    private val logger = LoggerFactory.getLogger("CobbleBugFix")
    private val reportedKeys = ConcurrentHashMap.newKeySet<String>()
    private val emptyPart = ModelPart(emptyList(), emptyMap())

    fun emptyModelPart(): ModelPart = emptyPart

    fun report(modelClassName: String, partName: String?, context: RenderContext?) {
        val safePartName = partName ?: "<null>"
        val entity = context?.entity
        val pokemon = (entity as? PokemonEntity)?.pokemon
        val species = pokemon?.species?.translatedName?.string
            ?: context?.let { tryGetContextValue(it, "access\$getSPECIES\$cp") }?.toString()
        val aspects = pokemon?.aspects?.toString()
            ?: context?.let { tryGetContextValue(it, "access\$getASPECTS\$cp") }?.toString()

        val key = buildString {
            append(modelClassName)
            append('|')
            append(safePartName)
            append('|')
            append(species ?: "<none>")
            append('|')
            append(aspects ?: "<none>")
        }
        if (!reportedKeys.add(key)) {
            return
        }

        val details = buildString {
            append("Cobblemon model part missing: part=")
            append(safePartName)
            append(", model=")
            append(modelClassName)
            if (species != null) {
                append(", species=")
                append(species)
            }
            if (aspects != null) {
                append(", aspects=")
                append(aspects)
            }
        }
        logger.warn(details)

        if (!CobbleBugFixConfig.shouldSendMissingReporter()) {
            return
        }

        val player = Minecraft.getInstance().player ?: return
        val message = buildString {
            append("[Cobblemon] Missing model part: ")
            append(safePartName)
            if (species != null) {
                append(", species=")
                append(species)
            }
            if (aspects != null) {
                append(", aspects=")
                append(aspects)
            }
        }
        player.displayClientMessage(Component.literal(message), false)
    }

    @Suppress("UNCHECKED_CAST")
    private fun tryGetContextValue(context: RenderContext, accessorName: String): Any? {
        return runCatching {
            val method = RenderContext::class.java.getMethod(accessorName)
            val key = method.invoke(null) as RenderContext.Key<Any>
            context.request(key)
        }.getOrNull()
    }
}
