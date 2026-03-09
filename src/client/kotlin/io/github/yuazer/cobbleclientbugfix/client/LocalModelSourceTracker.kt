package io.github.yuazer.cobbleclientbugfix.client

import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.FilePackResources
import net.minecraft.server.packs.PathPackResources
import net.minecraft.server.packs.resources.Resource
import java.io.File
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

object LocalModelSourceTracker {
    data class LocalModelSource(
        val speciesId: ResourceLocation,
        val jsonResource: ResourceLocation,
        val sourcePack: String,
        val localPath: String
    )

    private val modelSourceBySpecies = ConcurrentHashMap<ResourceLocation, LocalModelSource>()

    @JvmStatic
    fun clear() {
        modelSourceBySpecies.clear()
    }

    @JvmStatic
    fun record(speciesId: ResourceLocation, jsonResource: ResourceLocation, resource: Resource) {
        modelSourceBySpecies[speciesId] = LocalModelSource(
            speciesId = speciesId,
            jsonResource = jsonResource,
            sourcePack = resolvePackId(resource),
            localPath = resolveLocalPath(resource, jsonResource)
        )
    }

    @JvmStatic
    fun getSource(speciesId: ResourceLocation): LocalModelSource? = modelSourceBySpecies[speciesId]

    private fun resolvePackId(resource: Resource): String {
        return runCatching { resource.sourcePackId() }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: runCatching { resource.source().packId() }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: "unknown"
    }

    private fun resolveLocalPath(resource: Resource, jsonResource: ResourceLocation): String {
        val source = resource.source()
        val insidePath = "assets/${jsonResource.namespace}/${jsonResource.path}"

        if (source is PathPackResources) {
            val root = getFieldValueByType<Path>(source)
            if (root != null) {
                return root.resolve(insidePath).toAbsolutePath().normalize().toString()
            }
        }

        if (source is FilePackResources) {
            val zipAccess = getFieldValueByType<Any>(source) { fieldType ->
                fieldType.name.contains("SharedZipFileAccess")
            }
            val file = zipAccess?.let { getFieldValueByType<File>(it) }
            if (file != null) {
                return file.absolutePath + "!/" + insidePath.replace('\\', '/')
            }
        }

        return "unknown"
    }

    private inline fun <reified T> getFieldValueByType(
        instance: Any,
        crossinline predicate: (Class<*>) -> Boolean = { type -> T::class.java.isAssignableFrom(type) }
    ): T? {
        var current: Class<*>? = instance.javaClass
        while (current != null && current != Any::class.java) {
            val match = current.declaredFields.firstOrNull { predicate(it.type) }
            if (match == null) {
                current = current.superclass
                continue
            }
            return runCatching {
                match.isAccessible = true
                match.get(instance) as? T
            }.getOrNull()
        }
        return null
    }
}
