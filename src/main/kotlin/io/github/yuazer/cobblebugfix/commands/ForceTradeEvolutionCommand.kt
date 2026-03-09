package io.github.yuazer.cobblebugfix.commands

import com.cobblemon.mod.common.util.party
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import io.github.yuazer.cobblebugfix.config.CobbleBugFixConfig
import io.github.yuazer.cobblebugfix.util.PokemonUtil
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component

object ForceTradeEvolutionCommand {

    private const val COMMAND_NAME = "cbfforcetradeevo"

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal(COMMAND_NAME)
                .requires { it.hasPermission(2) }
                .then(
                    Commands.argument("player", EntityArgument.player())
                        .then(
                            Commands.argument("slot", IntegerArgumentType.integer(1, 6))
                                .then(
                                    Commands.argument("evolutionId", StringArgumentType.greedyString())
                                        .executes { ctx ->
                                            val target = EntityArgument.getPlayer(ctx, "player")
                                            val slot = IntegerArgumentType.getInteger(ctx, "slot")
                                            val evolutionId = StringArgumentType.getString(ctx, "evolutionId")

                                            val pokemon = target.party().get(slot - 1)
                                            if (pokemon == null) {
                                                ctx.source.sendFailure(
                                                    Component.literal(
                                                        CobbleBugFixConfig.getMessage(
                                                            key = "forceTrade.slotEmpty",
                                                            default = "槽位 {slot} 为空，无法强制进化。",
                                                            placeholders = mapOf("slot" to slot.toString())
                                                        )
                                                    )
                                                )
                                                return@executes 0
                                            }

                                            val success = PokemonUtil.forceTradeEvolution(pokemon, evolutionId)
                                            if (success) {
                                                ctx.source.sendSuccess(
                                                    {
                                                        Component.literal(
                                                            CobbleBugFixConfig.getMessage(
                                                                key = "forceTrade.success",
                                                                default = "已为 {player} 的槽位 {slot} 强制触发交易进化：{evolutionId}",
                                                                placeholders = mapOf(
                                                                    "player" to target.scoreboardName,
                                                                    "slot" to slot.toString(),
                                                                    "evolutionId" to evolutionId
                                                                )
                                                            )
                                                        )
                                                    },
                                                    true
                                                )
                                                1
                                            } else {
                                                ctx.source.sendFailure(
                                                    Component.literal(
                                                        CobbleBugFixConfig.getMessage(
                                                            key = "forceTrade.notFound",
                                                            default = "未找到匹配的交易进化：{evolutionId}",
                                                            placeholders = mapOf("evolutionId" to evolutionId)
                                                        )
                                                    )
                                                )
                                                0
                                            }
                                        }
                                )
                        )
                )
        )
    }
}
