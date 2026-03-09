package io.github.yuazer.cobblebugfix.commands

import com.cobblemon.mod.common.api.abilities.Abilities
import com.cobblemon.mod.common.util.asTranslated
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.google.gson.GsonBuilder
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

object ExportAbilitiesCommand {
    private const val COMMAND_NAME = "cbfexportabilities"
    private const val DEFAULT_CSV_OUTPUT = "config/cobblebugfix/abilities_export.csv"
    private const val DEFAULT_JSON_OUTPUT = "config/cobblebugfix/abilities_export.json"
    private val LOGGER = LoggerFactory.getLogger("CobbleBugFix/ExportAbilities")
    private val GSON = GsonBuilder().setPrettyPrinting().create()

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal(COMMAND_NAME)
                .requires { it.hasPermission(2) }
                .then(
                    Commands.literal("csv")
                        .executes { ctx -> exportAbilitiesCsv(ctx.source, DEFAULT_CSV_OUTPUT) }
                        .then(
                            Commands.argument("path", StringArgumentType.greedyString())
                                .executes { ctx ->
                                    exportAbilitiesCsv(ctx.source, StringArgumentType.getString(ctx, "path"))
                                }
                        )
                )
                .then(
                    Commands.literal("json")
                        .executes { ctx -> exportAbilitiesJson(ctx.source, DEFAULT_JSON_OUTPUT) }
                        .then(
                            Commands.argument("path", StringArgumentType.greedyString())
                                .executes { ctx ->
                                    exportAbilitiesJson(ctx.source, StringArgumentType.getString(ctx, "path"))
                                }
                        )
                )
        )
    }

    private fun exportAbilitiesCsv(source: CommandSourceStack, rawPath: String): Int {
        return try {
            val outputPath = resolveOutputPath(rawPath)
            Files.createDirectories(outputPath.parent)

            val abilities = Abilities.all().sortedBy { it.name }
            val lines = mutableListOf<String>()
            lines += "# generated_at,${LocalDateTime.now()}"
            lines += "name,translated_name,translation_key"
            for (ability in abilities) {
                val key = ability.displayName
                val translated = key.asTranslated().string
                lines += "\"${escapeCsv(ability.name)}\",\"${escapeCsv(translated)}\",\"${escapeCsv(key)}\""
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
                {
                    Component.literal(
                        "Exported ${abilities.size} abilities to CSV: ${outputPath.toAbsolutePath()}"
                    )
                },
                false
            )
            1
        } catch (e: Exception) {
            LOGGER.error("Failed to export abilities CSV", e)
            source.sendFailure(Component.literal("Failed to export abilities CSV: ${e.message ?: "unknown error"}"))
            0
        }
    }

    private fun exportAbilitiesJson(source: CommandSourceStack, rawPath: String): Int {
        return try {
            val outputPath = resolveOutputPath(rawPath)
            Files.createDirectories(outputPath.parent)

            val abilities = Abilities.all().sortedBy { it.name }
            val payload = mapOf(
                "generated_at" to LocalDateTime.now().toString(),
                "count" to abilities.size,
                "abilities" to abilities.map { ability ->
                    val key = ability.displayName
                    mapOf(
                        "name" to ability.name,
                        "translated_name" to key.asTranslated().string,
                        "translation_key" to key
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
                {
                    Component.literal(
                        "Exported ${abilities.size} abilities to JSON: ${outputPath.toAbsolutePath()}"
                    )
                },
                false
            )
            1
        } catch (e: Exception) {
            LOGGER.error("Failed to export abilities JSON", e)
            source.sendFailure(Component.literal("Failed to export abilities JSON: ${e.message ?: "unknown error"}"))
            0
        }
    }

    private fun resolveOutputPath(rawPath: String): Path {
        val inputPath = Paths.get(rawPath).normalize()
        return if (inputPath.isAbsolute) inputPath else Paths.get("").toAbsolutePath().resolve(inputPath).normalize()
    }

    private fun escapeCsv(value: String): String {
        return value.replace("\"", "\"\"")
    }
}
