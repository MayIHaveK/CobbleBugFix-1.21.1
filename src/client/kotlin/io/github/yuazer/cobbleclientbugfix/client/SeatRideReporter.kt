package io.github.yuazer.cobbleclientbugfix.client

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import io.github.yuazer.cobblebugfix.config.CobbleBugFixConfig
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

object SeatRideReporter {
    private val logger = LoggerFactory.getLogger("CobbleBugFix")
    private val reportedKeys = ConcurrentHashMap.newKeySet<String>()

    fun report(entity: PokemonEntity, passengerName: String) {
        val species = entity.pokemon.species.translatedName.string
        val aspects = entity.pokemon.aspects.toString()
        val key = buildString {
            append(entity.uuid)
            append('|')
            append(passengerName)
            append('|')
            append(species)
            append('|')
            append(aspects)
        }
        if (!reportedKeys.add(key)) {
            return
        }

        val details = buildString {
            append("Cobblemon seat mismatch: passenger=")
            append(passengerName)
            append(", pokemon=")
            append(entity.uuid)
            append(", species=")
            append(species)
            append(", aspects=")
            append(aspects)
        }
        logger.warn(details)

        if (!CobbleBugFixConfig.shouldSendSeatReporter()) {
            return
        }

        val player = Minecraft.getInstance().player ?: return
        player.displayClientMessage(
            Component.literal(
                "[Cobblemon] Seat mismatch blocked: passenger=$passengerName, species=$species, aspects=$aspects"
            ),
            false
        )
    }
}
