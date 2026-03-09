package io.github.yuazer.cobblebugfix.commands

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies
import com.cobblemon.mod.common.pokemon.Species
import com.google.gson.GsonBuilder
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime

object ExportSpeciesCommand {
    private const val COMMAND_NAME = "cbfexportspecies"
    private const val DEFAULT_CSV_OUTPUT = "config/cobblebugfix/species_export.csv"
    private const val DEFAULT_JSON_OUTPUT = "config/cobblebugfix/species_export.json"
    private val LOGGER = LoggerFactory.getLogger("CobbleBugFix/ExportSpecies")
    private val GSON = GsonBuilder().setPrettyPrinting().create()

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal(COMMAND_NAME)
                .requires { it.hasPermission(2) }
                .then(
                    Commands.literal("csv")
                        .executes { ctx -> exportSpeciesCsv(ctx.source, DEFAULT_CSV_OUTPUT) }
                        .then(
                            Commands.argument("path", StringArgumentType.greedyString())
                                .executes { ctx ->
                                    exportSpeciesCsv(ctx.source, StringArgumentType.getString(ctx, "path"))
                                }
                        )
                )
                .then(
                    Commands.literal("json")
                        .executes { ctx -> exportSpeciesJson(ctx.source, DEFAULT_JSON_OUTPUT) }
                        .then(
                            Commands.argument("path", StringArgumentType.greedyString())
                                .executes { ctx ->
                                    exportSpeciesJson(ctx.source, StringArgumentType.getString(ctx, "path"))
                                }
                        )
                )
        )
    }

    private fun exportSpeciesCsv(source: CommandSourceStack, rawPath: String): Int {
        return try {
            val outputPath = resolveOutputPath(rawPath)
            Files.createDirectories(outputPath.parent)

            val speciesList = PokemonSpecies.species.sortedBy { it.resourceIdentifier.toString() }
            val lines = mutableListOf<String>()
            lines += "# generated_at,${LocalDateTime.now()}"
            lines += "species_id,translated_name,translation_key"
            for (species in speciesList) {
                val speciesId = species.resourceIdentifier.path
                val translated = species.translatedName.string
                val translationKey = translationKeyOf(species)
                lines += "\"${escapeCsv(speciesId)}\",\"${escapeCsv(translated)}\",\"${escapeCsv(translationKey)}\""
            }

            Files.write(
                outputPath,
                lines,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
            )

            source.sendSuccess(
                { Component.literal("Exported ${speciesList.size} species to CSV: ${outputPath.toAbsolutePath()}") },
                false
            )
            1
        } catch (e: Exception) {
            LOGGER.error("Failed to export species CSV", e)
            source.sendFailure(Component.literal("Failed to export species CSV: ${e.message ?: "unknown error"}"))
            0
        }
    }

    private fun exportSpeciesJson(source: CommandSourceStack, rawPath: String): Int {
        return try {
            val outputPath = resolveOutputPath(rawPath)
            Files.createDirectories(outputPath.parent)

            val speciesList = PokemonSpecies.species.sortedBy { it.resourceIdentifier.toString() }
            val payload = mapOf(
                "generated_at" to LocalDateTime.now().toString(),
                "count" to speciesList.size,
                "species" to speciesList.map { species ->
                    mapOf(
                        "species_id" to species.resourceIdentifier.path,
                        "translated_name" to species.translatedName.string,
                        "translation_key" to translationKeyOf(species)
                    )
                }
            )

            Files.writeString(
                outputPath,
                GSON.toJson(payload),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
            )

            source.sendSuccess(
                { Component.literal("Exported ${speciesList.size} species to JSON: ${outputPath.toAbsolutePath()}") },
                false
            )
            1
        } catch (e: Exception) {
            LOGGER.error("Failed to export species JSON", e)
            source.sendFailure(Component.literal("Failed to export species JSON: ${e.message ?: "unknown error"}"))
            0
        }
    }

    private fun translationKeyOf(species: Species): String {
        return "${species.resourceIdentifier.namespace}.species.${species.resourceIdentifier.path}.name"
    }

    private fun resolveOutputPath(rawPath: String): Path {
        val inputPath = Paths.get(rawPath).normalize()
        return if (inputPath.isAbsolute) inputPath else Paths.get("").toAbsolutePath().resolve(inputPath).normalize()
    }

    private fun escapeCsv(value: String): String = value.replace("\"", "\"\"")
}
