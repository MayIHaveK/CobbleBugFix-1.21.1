package io.github.yuazer.cobblebugfix.commands

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.storage.player.PlayerInstancedDataStoreTypes
import com.mojang.brigadier.CommandDispatcher
import io.github.yuazer.cobblebugfix.config.CobbleBugFixConfig
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component

object ResetStarterStateCommand {

    private const val COMMAND_NAME = "cbfresetstarter"

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal(COMMAND_NAME)
                .requires { it.hasPermission(2) }
                .then(
                    Commands.argument("player", EntityArgument.player())
                        .executes { ctx ->
                            val target = EntityArgument.getPlayer(ctx, "player")
                            val playerData = Cobblemon.playerDataManager.getGenericData(target)

                            playerData.starterSelected = false
                            playerData.starterPrompted = false
                            playerData.starterUUID = null
                            playerData.starterLocked = false

                            Cobblemon.playerDataManager.saveSingle(playerData, PlayerInstancedDataStoreTypes.GENERAL)
                            playerData.sendToPlayer(target)

                            ctx.source.sendSuccess(
                                {
                                    Component.literal(
                                        CobbleBugFixConfig.getMessage(
                                            key = "resetStarter.success",
                                            default = "已重置玩家 {player} 的初始精灵选择状态为未选择。",
                                            placeholders = mapOf("player" to target.scoreboardName)
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
}
