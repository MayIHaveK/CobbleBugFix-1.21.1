package io.github.yuazer.cobblebugfix.commands

import com.mojang.brigadier.CommandDispatcher
import io.github.yuazer.cobblebugfix.config.CobbleBugFixConfig
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component

object ReloadConfigCommand {
    private const val COMMAND_NAME = "cbfreloadconfig"

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal(COMMAND_NAME)
                .requires { it.hasPermission(2) }
                .executes { ctx ->
                    CobbleBugFixConfig.load()
                    ctx.source.sendSuccess(
                        {
                            Component.literal(
                                CobbleBugFixConfig.getMessage(
                                    key = "reloadConfig.success",
                                    default = "CobbleBugFix 配置已重新加载，当前世界列表：{worlds}",
                                    placeholders = mapOf(
                                        "worlds" to CobbleBugFixConfig.getConfiguredWorlds().toString()
                                    )
                                )
                            )
                        },
                        true
                    )
                    1
                }
        )
    }
}
