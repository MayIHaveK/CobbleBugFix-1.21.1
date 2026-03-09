package io.github.yuazer.cobblebugfix.commands

import com.cobblemon.mod.common.util.party
import com.mojang.brigadier.CommandDispatcher
import io.github.yuazer.cobblebugfix.config.CobbleBugFixConfig
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer

object ClearPCCommand {

    private const val COMMAND_NAME = "cbfclearpc"

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal(COMMAND_NAME)
                .requires { it.hasPermission(2) }
                .then(
                    Commands.argument("player", EntityArgument.player())
                        .executes { ctx ->
                            val target = EntityArgument.getPlayer(ctx, "player")
                            val success = clearPC(target)

                            if (success) {
                                ctx.source.sendSuccess(
                                    {
                                        Component.literal(
                                            CobbleBugFixConfig.getMessage(
                                                key = "clearPc.success",
                                                default = "已成功清空玩家 {player} 的宝可梦PC。",
                                                placeholders = mapOf("player" to target.scoreboardName)
                                            )
                                        )
                                    },
                                    true
                                )
                            } else {
                                ctx.source.sendFailure(
                                    Component.literal(
                                        CobbleBugFixConfig.getMessage(
                                            key = "clearPc.failNoPc",
                                            default = "无法清空玩家 {player} 的PC（PC不存在）。",
                                            placeholders = mapOf("player" to target.scoreboardName)
                                        )
                                    )
                                )
                            }
                            1
                        }
                )
        )
    }

    private fun clearPC(player: ServerPlayer): Boolean {
        val pc = player.party().getOverflowPC(player.registryAccess()) ?: return false
        pc.clearPC()
        return true
    }
}
