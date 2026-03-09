package io.github.yuazer.cobblebugfix.handler

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.properties.CustomPokemonProperty
import com.cobblemon.mod.common.api.storage.player.PlayerInstancedDataStoreTypes
import com.cobblemon.mod.common.platform.events.PlatformEvents
import io.github.yuazer.cobblebugfix.config.CobbleBugFixConfig
import io.github.yuazer.cobblebugfix.network.ClientModelSourceService
import io.github.yuazer.cobblebugfix.properties.ForceMovesPropertyType
import io.github.yuazer.cobblebugfix.properties.LevelRangePropertyType
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object ServerHandler {
    private const val STARTER_CHECK_INTERVAL_TICKS = 5
    private val starterPromptedThisSession = ConcurrentHashMap.newKeySet<UUID>()

    fun register() {
        PlatformEvents.SERVER_STARTED.subscribe {
            registerCustomProperties()
        }
        PlatformEvents.SERVER_PLAYER_LOGIN.subscribe {
            starterPromptedThisSession.remove(it.player.uuid)
        }
        PlatformEvents.SERVER_PLAYER_LOGOUT.subscribe {
            starterPromptedThisSession.remove(it.player.uuid)
            ClientModelSourceService.clearPlayer(it.player.uuid)
        }
        PlatformEvents.SERVER_PLAYER_TICK_POST.subscribe {
            enforceStarterChoiceIfNeeded(it.player)
        }
    }

    fun registerCustomProperties() {
        CustomPokemonProperty.register(LevelRangePropertyType)
        CustomPokemonProperty.register(ForceMovesPropertyType)
    }

    private fun enforceStarterChoiceIfNeeded(player: net.minecraft.server.level.ServerPlayer) {
        if (!CobbleBugFixConfig.shouldForceChooseStarterOnFirstJoin()) {
            return
        }
        if (player.tickCount % STARTER_CHECK_INTERVAL_TICKS != 0) {
            return
        }

        val playerData = Cobblemon.playerDataManager.getGenericData(player)
        if (playerData.starterSelected) {
            starterPromptedThisSession.remove(player.uuid)
            return
        }

        if (playerData.starterLocked) {
            playerData.starterLocked = false
            Cobblemon.playerDataManager.saveSingle(playerData, PlayerInstancedDataStoreTypes.GENERAL)
            playerData.sendToPlayer(player)
        }

        if (starterPromptedThisSession.add(player.uuid)) {
            Cobblemon.starterHandler.requestStarterChoice(player)
        }
    }
}
