package io.github.yuazer.cobblebugfix.commands

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies
import com.cobblemon.mod.common.util.party
import com.mojang.brigadier.CommandDispatcher
import io.github.yuazer.cobblebugfix.config.CobbleBugFixConfig
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer

object GiveAllPokemon {
    private const val COMMAND_NAME = "cbfgiveallpokemon"

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal(COMMAND_NAME)
                .requires { it.hasPermission(2) }
                .then(
                    Commands.argument("player", EntityArgument.player())
                        .executes { ctx ->
                            val target = EntityArgument.getPlayer(ctx, "player")
                            val count = giveAllPokemon(target)

                            ctx.source.sendSuccess(
                                {
                                    Component.literal(
                                        CobbleBugFixConfig.getMessage(
                                            key = "giveAllPokemon.success",
                                            default = "已向玩家 {player} 的PC发放全部宝可梦，共 {count} 只。",
                                            placeholders = mapOf(
                                                "player" to target.scoreboardName,
                                                "count" to count.toString()
                                            )
                                        )
                                    )
                                },
                                true
                            )
                            1
                        }
                )
        )
    }

    private fun giveAllPokemon(player: ServerPlayer): Int {
        val pc = player.party().getOverflowPC(player.registryAccess()) ?: return 0
        val orderedSpecies = PokemonSpecies.implemented.sortedBy { it.nationalPokedexNumber }

        var count = 0
        for (species in orderedSpecies) {
            val pokemon = species.create()
            val displayName = pokemon.getDisplayName(false).string
            if (displayName.contains("cobblemon")) {
                println("Pokemon ${pokemon.species.name} is not localized.")
            }
            pokemon.setOriginalTrainer(player.uuid)
            pc.add(pokemon)
            count++
        }
        return count
    }
}
