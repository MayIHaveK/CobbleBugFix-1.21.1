package io.github.yuazer.cobblebugfix

import io.github.yuazer.cobblebugfix.commands.ClearPCCommand
import io.github.yuazer.cobblebugfix.commands.ExportAbilitiesCommand
import io.github.yuazer.cobblebugfix.commands.ExportMovesCommand
import io.github.yuazer.cobblebugfix.commands.ExportSpeciesCommand
import io.github.yuazer.cobblebugfix.commands.ForceTradeEvolutionCommand
import io.github.yuazer.cobblebugfix.commands.GiveAllPokemon
import io.github.yuazer.cobblebugfix.commands.ListEvolutionsCommand
import io.github.yuazer.cobblebugfix.commands.PokemonSourceCommand
import io.github.yuazer.cobblebugfix.commands.ReloadConfigCommand
import io.github.yuazer.cobblebugfix.commands.ResetStarterStateCommand
import io.github.yuazer.cobblebugfix.config.CobbleBugFixConfig
import io.github.yuazer.cobblebugfix.handler.ServerHandler
import io.github.yuazer.cobblebugfix.network.ClientModelSourceService
import io.github.yuazer.cobblebugfix.network.ModelSourcePackets
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import org.slf4j.LoggerFactory

class Cobblebugfix : ModInitializer {
    override fun onInitialize() {
        CobbleBugFixConfig.load()
        ModelSourcePackets.registerPayloadTypes()
        ClientModelSourceService.registerServerReceiver()
        CommandRegistrationCallback.EVENT.register(CommandRegistrationCallback { dispatcher, _, _ ->
            GiveAllPokemon.register(dispatcher)
            ClearPCCommand.register(dispatcher)
            ForceTradeEvolutionCommand.register(dispatcher)
            ReloadConfigCommand.register(dispatcher)
            ListEvolutionsCommand.register(dispatcher)
            ResetStarterStateCommand.register(dispatcher)
            PokemonSourceCommand.register(dispatcher)
            ExportAbilitiesCommand.register(dispatcher)
            ExportMovesCommand.register(dispatcher)
            ExportSpeciesCommand.register(dispatcher)
        })
        ServerHandler.register()
        val logger = LoggerFactory.getLogger("CobbleBugFix")
        logger.info("CobbleBugFix initialized")
    }
}
