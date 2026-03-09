package io.github.yuazer.cobblebugfix.network

import io.github.yuazer.cobblebugfix.network.ModelSourcePackets.ModelSourceRequestPayload
import io.github.yuazer.cobblebugfix.network.ModelSourcePackets.ModelSourceResponsePayload
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object ClientModelSourceService {
    data class ClientModelSource(
        val speciesId: ResourceLocation,
        val jsonResource: String,
        val sourcePack: String,
        val localPath: String
    )

    private val sourceCache = ConcurrentHashMap<UUID, ConcurrentHashMap<ResourceLocation, ClientModelSource>>()
    private val pendingQuery = ConcurrentHashMap<UUID, MutableSet<ResourceLocation>>()

    @JvmStatic
    fun registerServerReceiver() {
        ServerPlayNetworking.registerGlobalReceiver(ModelSourceResponsePayload.TYPE) { payload, context ->
            val player = context.player()
            sourceCache.computeIfAbsent(player.uuid) { ConcurrentHashMap() }[payload.speciesId] = ClientModelSource(
                speciesId = payload.speciesId,
                jsonResource = payload.jsonResource,
                sourcePack = payload.sourcePack,
                localPath = payload.localPath
            )

            val pending = pendingQuery[player.uuid] ?: return@registerGlobalReceiver
            if (!pending.remove(payload.speciesId)) {
                return@registerGlobalReceiver
            }

            player.server.execute {
                player.sendSystemMessage(
                    Component.literal(
                        "Client model source: resource=${payload.jsonResource}, pack=${payload.sourcePack}, path=${payload.localPath}"
                    )
                )
            }
        }
    }

    @JvmStatic
    fun requestFromClient(player: ServerPlayer, speciesId: ResourceLocation): Boolean {
        if (!ServerPlayNetworking.canSend(player, ModelSourceRequestPayload.TYPE)) {
            return false
        }
        pendingQuery.computeIfAbsent(player.uuid) { ConcurrentHashMap.newKeySet<ResourceLocation>() }.add(speciesId)
        ServerPlayNetworking.send(player, ModelSourceRequestPayload(speciesId))
        return true
    }

    @JvmStatic
    fun getCached(player: ServerPlayer, speciesId: ResourceLocation): ClientModelSource? {
        return sourceCache[player.uuid]?.get(speciesId)
    }

    @JvmStatic
    fun clearPlayer(playerId: UUID) {
        sourceCache.remove(playerId)
        pendingQuery.remove(playerId)
    }
}
