package io.github.yuazer.cobbleclientbugfix.mixin.client

import com.cobblemon.mod.common.client.render.models.blockbench.repository.VaryingModelRepository
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.yuazer.cobbleclientbugfix.client.LocalModelSourceTracker
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(VaryingModelRepository::class)
abstract class VaryingModelRepositorySourceTrackerMixin {
    @Inject(method = ["reload"], at = [At("TAIL")])
    private fun cobblebugfix_trackModelSource(resourceManager: ResourceManager, ci: CallbackInfo) {
        LocalModelSourceTracker.clear()

        val directories = listOf(
            "bedrock/species",
            "bedrock/pokemon/resolvers",
            "bedrock/pokemon/variations"
        )

        for (directory in directories) {
            resourceManager
                .listResources(directory) { path -> path.path.endsWith(".json") }
                .forEach { (identifier, resource) ->
                    val speciesId = resolveSpecies(identifier, resource) ?: return@forEach
                    LocalModelSourceTracker.record(speciesId, identifier, resource)
                }
        }
    }

    private fun resolveSpecies(jsonResource: ResourceLocation, resource: net.minecraft.server.packs.resources.Resource): ResourceLocation? {
        try {
            resource.openAsReader().use { reader ->
                val root = JsonParser.parseReader(reader).asJsonObject
                parseSpecies(root)?.let { return it }
            }
        } catch (_: Exception) {
        }
        return ResourceLocation(jsonResource.namespace, jsonResource.path.substringAfterLast('/').removeSuffix(".json"))
    }

    private fun parseSpecies(root: JsonObject): ResourceLocation? {
        val value = when {
            root.has("species") -> root.get("species").asString
            root.has("name") -> root.get("name").asString
            else -> null
        } ?: return null
        return parseResourceLocation(value)
    }

    private fun parseResourceLocation(raw: String): ResourceLocation? {
        val text = raw.trim()
        if (text.isEmpty()) return null
        val split = text.split(':', limit = 2)
        return try {
            when (split.size) {
                1 -> ResourceLocation("cobblemon", split[0])
                2 -> ResourceLocation(split[0], split[1])
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }
}
