package io.github.yuazer.cobblebugfix.commands

import com.cobblemon.mod.common.api.moves.Moves
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

object ExportMovesCommand {
    private const val COMMAND_NAME = "cbfexportmoves"
    private const val DEFAULT_CSV_OUTPUT = "config/cobblebugfix/moves_export.csv"
    private const val DEFAULT_JSON_OUTPUT = "config/cobblebugfix/moves_export.json"
    private val LOGGER = LoggerFactory.getLogger("CobbleBugFix/ExportMoves")
    private val GSON = GsonBuilder().setPrettyPrinting().create()

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal(COMMAND_NAME)
                .requires { it.hasPermission(2) }
                .then(
                    Commands.literal("csv")
                        .executes { ctx -> exportMovesCsv(ctx.source, DEFAULT_CSV_OUTPUT) }
                        .then(
                            Commands.argument("path", StringArgumentType.greedyString())
                                .executes { ctx ->
                                    exportMovesCsv(ctx.source, StringArgumentType.getString(ctx, "path"))
                                }
                        )
                )
                .then(
                    Commands.literal("json")
                        .executes { ctx -> exportMovesJson(ctx.source, DEFAULT_JSON_OUTPUT) }
                        .then(
                            Commands.argument("path", StringArgumentType.greedyString())
                                .executes { ctx ->
                                    exportMovesJson(ctx.source, StringArgumentType.getString(ctx, "path"))
                                }
                        )
                )
        )
    }

    private fun exportMovesCsv(source: CommandSourceStack, rawPath: String): Int {
        return try {
            val outputPath = resolveOutputPath(rawPath)
            Files.createDirectories(outputPath.parent)

            val moves = Moves.all().sortedBy { it.name }
            val lines = mutableListOf<String>()
            lines += "# generated_at,${LocalDateTime.now()}"
            lines += "name,translated_name,translation_key"
            for (move in moves) {
                val translationKey = "cobblemon.move.${move.name}"
                val translated = move.displayName.string
                lines += "\"${escapeCsv(move.name)}\",\"${escapeCsv(translated)}\",\"${escapeCsv(translationKey)}\""
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
                { Component.literal("Exported ${moves.size} moves to CSV: ${outputPath.toAbsolutePath()}") },
                false
            )
            1
        } catch (e: Exception) {
            LOGGER.error("Failed to export moves CSV", e)
            source.sendFailure(Component.literal("Failed to export moves CSV: ${e.message ?: "unknown error"}"))
            0
        }
    }

    private fun exportMovesJson(source: CommandSourceStack, rawPath: String): Int {
        return try {
            val outputPath = resolveOutputPath(rawPath)
            Files.createDirectories(outputPath.parent)

            val moves = Moves.all().sortedBy { it.name }
            val payload = mapOf(
                "generated_at" to LocalDateTime.now().toString(),
                "count" to moves.size,
                "moves" to moves.map { move ->
                    mapOf(
                        "name" to move.name,
                        "translated_name" to move.displayName.string,
                        "translation_key" to "cobblemon.move.${move.name}"
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
                { Component.literal("Exported ${moves.size} moves to JSON: ${outputPath.toAbsolutePath()}") },
                false
            )
            1
        } catch (e: Exception) {
            LOGGER.error("Failed to export moves JSON", e)
            source.sendFailure(Component.literal("Failed to export moves JSON: ${e.message ?: "unknown error"}"))
            0
        }
    }

    private fun resolveOutputPath(rawPath: String): Path {
        val inputPath = Paths.get(rawPath).normalize()
        return if (inputPath.isAbsolute) inputPath else Paths.get("").toAbsolutePath().resolve(inputPath).normalize()
    }

    private fun escapeCsv(value: String): String = value.replace("\"", "\"\"")
}
