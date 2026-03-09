package io.github.yuazer.cobblebugfix.commands

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies
import com.cobblemon.mod.common.pokemon.Species
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import io.github.yuazer.cobblebugfix.config.CobbleBugFixConfig
import io.github.yuazer.cobblebugfix.network.ClientModelSourceService
import io.github.yuazer.cobblebugfix.tracker.PokemonSourceTracker
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer

object PokemonSourceCommand {
    private const val COMMAND_NAME = "cbfsource"

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal(COMMAND_NAME)
                .requires { it.hasPermission(2) }
                // /cbfsource <species> [species|riding|model]
                .then(
                    Commands.argument("species", StringArgumentType.word())
                        .executes { ctx ->
                            executeQuery(ctx.source, StringArgumentType.getString(ctx, "species"), "all")
                        }
                        .then(
                            Commands.literal("species")
                                .executes { ctx ->
                                    executeQuery(ctx.source, StringArgumentType.getString(ctx, "species"), "species")
                                }
                        )
                        .then(
                            Commands.literal("riding")
                                .executes { ctx ->
                                    executeQuery(ctx.source, StringArgumentType.getString(ctx, "species"), "riding")
                                }
                        )
                        .then(
                            Commands.literal("model")
                                .executes { ctx ->
                                    executeQuery(ctx.source, StringArgumentType.getString(ctx, "species"), "model")
                                }
                        )
                )
                // /cbfsource species|riding|model <species>
                .then(
                    Commands.literal("species")
                        .then(
                            Commands.argument("species", StringArgumentType.word())
                                .executes { ctx ->
                                    val speciesText = StringArgumentType.getString(ctx, "species")
                                    executeQuery(ctx.source, speciesText, "species")
                                }
                        )
                        // /cbfsource species model|riding|species <species>
                        .then(
                            Commands.literal("species")
                                .then(
                                    Commands.argument("species", StringArgumentType.word())
                                        .executes { ctx ->
                                            executeQuery(ctx.source, StringArgumentType.getString(ctx, "species"), "species")
                                        }
                                )
                        )
                        .then(
                            Commands.literal("riding")
                                .then(
                                    Commands.argument("species", StringArgumentType.word())
                                        .executes { ctx ->
                                            executeQuery(ctx.source, StringArgumentType.getString(ctx, "species"), "riding")
                                        }
                                )
                        )
                        .then(
                            Commands.literal("model")
                                .then(
                                    Commands.argument("species", StringArgumentType.word())
                                        .executes { ctx ->
                                            executeQuery(ctx.source, StringArgumentType.getString(ctx, "species"), "model")
                                        }
                                )
                        )
                )
                .then(
                    Commands.literal("riding")
                        .then(
                            Commands.argument("species", StringArgumentType.word())
                                .executes { ctx ->
                                    val speciesText = StringArgumentType.getString(ctx, "species")
                                    executeQuery(ctx.source, speciesText, "riding")
                                }
                        )
                )
                .then(
                    Commands.literal("model")
                        .then(
                            Commands.argument("species", StringArgumentType.word())
                                .executes { ctx ->
                                    val speciesText = StringArgumentType.getString(ctx, "species")
                                    executeQuery(ctx.source, speciesText, "model")
                                }
                        )
                )
        )
    }

    private fun executeQuery(source: CommandSourceStack, speciesText: String, mode: String): Int {
        val species = resolveSpecies(speciesText)
        if (species == null) {
            source.sendFailure(
                Component.literal(
                    CobbleBugFixConfig.getMessage(
                        key = "sourceQuery.speciesNotFound",
                        default = "Species not found: {species}",
                        placeholders = mapOf("species" to speciesText)
                    )
                )
            )
            return 0
        }

        val id = species.resourceIdentifier
        val speciesSource = PokemonSourceTracker.getSpeciesSource(id)
        val ridingSource = PokemonSourceTracker.getRidingSource(id)
        val modelSource = PokemonSourceTracker.getModelSource(id)
        val player = source.entity as? ServerPlayer
        val clientModelSource = player?.let { ClientModelSourceService.getCached(it, id) }

        val lines = mutableListOf<String>()
        lines += CobbleBugFixConfig.getMessage(
            key = "sourceQuery.header",
            default = "Pokemon source query: {species}",
            placeholders = mapOf("species" to id.toString())
        )

        if (mode == "all" || mode == "species") {
            lines += CobbleBugFixConfig.getMessage(
                key = "sourceQuery.speciesLine",
                default = "Species JSON source: resource={json}, pack={pack}",
                placeholders = mapOf(
                    "json" to (speciesSource?.jsonResource?.toString() ?: "unknown"),
                    "pack" to (speciesSource?.sourcePack ?: "unknown")
                )
            )
        }

        if (mode == "all" || mode == "riding") {
            lines += CobbleBugFixConfig.getMessage(
                key = "sourceQuery.ridingLine",
                default = "Riding source: pack={pack}, behaviours={behaviours}",
                placeholders = mapOf(
                    "pack" to (ridingSource?.sourcePack ?: "unknown"),
                    "behaviours" to (ridingSource?.behaviourKeys?.joinToString(", ").orEmpty().ifBlank { "none" })
                )
            )
        }

        if (mode == "all" || mode == "model") {
            val modelJson = clientModelSource?.jsonResource ?: (modelSource?.jsonResource?.toString() ?: "unknown")
            val modelPack = clientModelSource?.sourcePack ?: (modelSource?.sourcePack ?: "unknown")
            lines += CobbleBugFixConfig.getMessage(
                key = "sourceQuery.modelLine",
                default = "Model poser source: resource={json}, pack={pack}",
                placeholders = mapOf(
                    "json" to modelJson,
                    "pack" to modelPack
                )
            )

            if (clientModelSource != null) {
                lines += CobbleBugFixConfig.getMessage(
                    key = "sourceQuery.modelClientPathLine",
                    default = "Client local model file: {path}",
                    placeholders = mapOf("path" to clientModelSource.localPath)
                )
            } else if (player != null) {
                val requested = ClientModelSourceService.requestFromClient(player, id)
                lines += CobbleBugFixConfig.getMessage(
                    key = "sourceQuery.modelClientPendingLine",
                    default = if (requested) {
                        "Client local model file: querying client, check chat for async result."
                    } else {
                        "Client local model file: unavailable (client mod/channel not ready)."
                    }
                )
            }
        }

        source.sendSuccess({ Component.literal(lines.joinToString("\n")) }, false)
        return 1
    }

    private fun resolveSpecies(input: String): Species? {
        if (input.contains(":")) {
            val identifier = parseResourceLocation(input) ?: return null
            return PokemonSpecies.getByIdentifier(identifier)
        }

        PokemonSpecies.getByName(input)?.let { return it }
        return PokemonSpecies.species.firstOrNull { it.name.equals(input, ignoreCase = true) }
    }

    private fun parseResourceLocation(input: String): ResourceLocation? {
        return runCatching {
            val split = input.split(':', limit = 2)
            when (split.size) {
                1 -> ResourceLocation("minecraft", split[0])
                2 -> ResourceLocation(split[0], split[1])
                else -> null
            }
        }.getOrNull()
    }
}
