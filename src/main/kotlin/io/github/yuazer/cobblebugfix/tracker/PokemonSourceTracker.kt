package io.github.yuazer.cobblebugfix.tracker

import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.Resource
import java.util.concurrent.ConcurrentHashMap

object PokemonSourceTracker {
    data class SpeciesSource(
        val speciesId: ResourceLocation,
        val jsonResource: ResourceLocation,
        val sourcePack: String
    )

    data class RidingSource(
        val speciesId: ResourceLocation,
        val sourcePack: String,
        val behaviourKeys: List<String>
    )

    data class ModelSource(
        val modelId: ResourceLocation,
        val jsonResource: ResourceLocation,
        val sourcePack: String
    )

    private val speciesSourceMap = ConcurrentHashMap<ResourceLocation, SpeciesSource>()
    private val ridingSourceMap = ConcurrentHashMap<ResourceLocation, RidingSource>()
    private val modelSourceMap = ConcurrentHashMap<ResourceLocation, ModelSource>()

    @JvmStatic
    fun clearSpeciesTracking() {
        speciesSourceMap.clear()
        ridingSourceMap.clear()
    }

    @JvmStatic
    fun clearModelTracking() {
        modelSourceMap.clear()
    }

    @JvmStatic
    fun recordSpeciesJsonSource(speciesId: ResourceLocation, jsonResource: ResourceLocation, resource: Resource?) {
        speciesSourceMap[speciesId] = SpeciesSource(
            speciesId = speciesId,
            jsonResource = jsonResource,
            sourcePack = resolveSourcePack(resource)
        )
    }

    @JvmStatic
    fun recordRidingSource(speciesId: ResourceLocation, behaviourKeys: List<String>) {
        recordRidingSource(speciesId, behaviourKeys, null)
    }

    @JvmStatic
    fun recordRidingSource(speciesId: ResourceLocation, behaviourKeys: List<String>, sourceResource: Resource?) {
        val source = speciesSourceMap[speciesId]
        ridingSourceMap[speciesId] = RidingSource(
            speciesId = speciesId,
            sourcePack = sourceResource?.let(::resolveSourcePack) ?: source?.sourcePack ?: "unknown",
            behaviourKeys = behaviourKeys
        )
    }

    @JvmStatic
    fun recordModelJsonSource(modelId: ResourceLocation, jsonResource: ResourceLocation, resource: Resource?) {
        recordModelJsonSource(modelId, jsonResource, resolveSourcePack(resource))
    }

    @JvmStatic
    fun recordModelJsonSource(modelId: ResourceLocation, jsonResource: ResourceLocation, sourcePack: String) {
        modelSourceMap[modelId] = ModelSource(
            modelId = modelId,
            jsonResource = jsonResource,
            sourcePack = sourcePack.ifBlank { "unknown" }
        )
    }

    @JvmStatic
    fun getSpeciesSource(speciesId: ResourceLocation): SpeciesSource? = speciesSourceMap[speciesId]

    @JvmStatic
    fun getRidingSource(speciesId: ResourceLocation): RidingSource? = ridingSourceMap[speciesId]

    @JvmStatic
    fun getModelSource(speciesId: ResourceLocation): ModelSource? = modelSourceMap[speciesId]

    private fun resolveSourcePack(resource: Resource?): String {
        if (resource == null) {
            return "unknown"
        }
        runCatching { resource.sourcePackId() }.getOrNull()?.takeIf { it.isNotBlank() }?.let { return it }
        runCatching { resource.source().packId() }.getOrNull()?.takeIf { it.isNotBlank() }?.let { return it }
        runCatching { resource.knownPackInfo().orElse(null) }
            .getOrNull()
            ?.let { known -> return "${known.namespace()}:${known.id()}" }
        return "unknown"
    }
}
