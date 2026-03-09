package io.github.yuazer.cobblebugfix.commands

import com.cobblemon.mod.common.util.party
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import io.github.yuazer.cobblebugfix.config.CobbleBugFixConfig
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component

object ListEvolutionsCommand {

    private const val COMMAND_NAME = "cbflistevos"

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal(COMMAND_NAME)
                .requires { it.hasPermission(2) }
                .then(
                    Commands.argument("player", EntityArgument.player())
                        .then(
                            Commands.argument("slot", IntegerArgumentType.integer(1, 6))
                                .executes { ctx ->
                                    val target = EntityArgument.getPlayer(ctx, "player")
                                    val slot = IntegerArgumentType.getInteger(ctx, "slot")

                                    val pokemon = target.party().get(slot - 1)
                                    if (pokemon == null) {
                                        ctx.source.sendFailure(
                                            Component.literal(
                                                CobbleBugFixConfig.getMessage(
                                                    key = "listEvos.slotEmpty",
                                                    default = "槽位 {slot} 为空，无法获取进化信息。",
                                                    placeholders = mapOf("slot" to slot.toString())
                                                )
                                            )
                                        )
                                        return@executes 0
                                    }

                                    val evolutionIds = pokemon.evolutions.map { it.id }
                                    if (evolutionIds.isEmpty()) {
                                        ctx.source.sendFailure(
                                            Component.literal(
                                                CobbleBugFixConfig.getMessage(
                                                    key = "listEvos.none",
                                                    default = "该精灵没有可用的进化。"
                                                )
                                            )
                                        )
                                        0
                                    } else {
                                        val message = Component.literal(
                                            CobbleBugFixConfig.getMessage(
                                                key = "listEvos.header",
                                                default = "该精灵的所有 evolutionId：\n"
                                            )
                                        )
                                        val prefix = CobbleBugFixConfig.getMessage(
                                            key = "listEvos.itemPrefix",
                                            default = "- "
                                        )
                                        evolutionIds.forEach { id ->
                                            message.append(Component.literal(prefix + id + "\n"))
                                        }
                                        ctx.source.sendSuccess({ message }, false)
                                        1
                                    }
                                }
                        )
                )
        )
    }
}
