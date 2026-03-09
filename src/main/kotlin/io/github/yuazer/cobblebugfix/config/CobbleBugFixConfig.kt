package io.github.yuazer.cobblebugfix.config

import io.github.yuazer.cobblebugfix.config.JsonConfigManager
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.Level
import org.slf4j.LoggerFactory

object CobbleBugFixConfig {
    private const val FISHING_SECTION = "fishingRodHookOverride"
    private const val BATTLE_CLIP_SECTION = "battleSendOutClipOverride"
    private const val NEED_CHOOSE_ON_FIRST_JOIN = "needChooseOnFirstJoin"
    private const val SEND_REPORTER_SEAT = "sendReporterSeat"
    private const val SEND_REPORTER_MISSING = "sendReporterMissing"
    private const val MESSAGE_SECTION = "Message"
    private val LOGGER = LoggerFactory.getLogger("CobbleBugFixConfig")

    @field:Config("cobblebugfix/config.json")
    lateinit var config: JsonConfig
        private set

    @JvmStatic
    fun shouldBypassEntityHooks(level: Level): Boolean {
        if (!::config.isInitialized) {
            return false
        }
        if (!config.getBoolean("$FISHING_SECTION.enabled", false)) {
            return false
        }
        val worlds = config.getStringList("$FISHING_SECTION.worlds")
        if (worlds.any { it.trim() == "*" }) {
            return true
        }
        val current = level.dimension().location()
        return worlds.any { matchesWorld(it, current) }
    }

    @JvmStatic
    fun isConfigLoaded(): Boolean = ::config.isInitialized

    @JvmStatic
    fun getConfiguredWorlds(): List<String> =
        if (!::config.isInitialized) emptyList() else config.getStringList("$FISHING_SECTION.worlds")

    @JvmStatic
    fun shouldSkipSendOutClip(): Boolean {
        if (!::config.isInitialized) {
            return false
        }
        return config.getBoolean("$BATTLE_CLIP_SECTION.enabled", false)
    }

    @JvmStatic
    fun shouldForceChooseStarterOnFirstJoin(): Boolean {
        if (!::config.isInitialized) {
            return true
        }
        return config.getBoolean(NEED_CHOOSE_ON_FIRST_JOIN, true)
    }

    @JvmStatic
    fun shouldSendSeatReporter(): Boolean {
        if (!::config.isInitialized) {
            return true
        }
        return config.getBoolean(SEND_REPORTER_SEAT, true)
    }

    @JvmStatic
    fun shouldSendMissingReporter(): Boolean {
        if (!::config.isInitialized) {
            return true
        }
        return config.getBoolean(SEND_REPORTER_MISSING, true)
    }

    @JvmStatic
    fun getMessage(key: String, default: String): String {
        if (!::config.isInitialized) {
            return default
        }
        return config.getString("$MESSAGE_SECTION.$key", default) ?: default
    }

    @JvmStatic
    fun getMessage(key: String, default: String, placeholders: Map<String, String>): String {
        var message = getMessage(key, default)
        placeholders.forEach { (name, value) ->
            message = message.replace("{$name}", value).replace("%$name%", value)
        }
        return message
    }

    @JvmStatic
    fun logConfigState() {
        if (!::config.isInitialized) {
            LOGGER.info("CobbleBugFix config is not initialized yet.")
            return
        }
        LOGGER.info(
            "CobbleBugFix config loaded (fishingEnabled={}, worlds={}, battleClipOverride={}, needChooseOnFirstJoin={})",
            config.getBoolean("$FISHING_SECTION.enabled", false),
            getConfiguredWorlds(),
            config.getBoolean("$BATTLE_CLIP_SECTION.enabled", false),
            shouldForceChooseStarterOnFirstJoin()
        )
    }

    @JvmStatic
    fun load() {
        if (!isConfigLoaded()) {
            JsonConfigManager.register(this)
        }
        logConfigState()
    }

    private fun matchesWorld(raw: String, current: ResourceLocation): Boolean {
        val sanitized = raw.trim()
        if (sanitized.isEmpty()) {
            return false
        }
        val candidate = parseLocation(sanitized) ?: return false
        return candidate == current
    }

    private fun parseLocation(value: String): ResourceLocation? {
        return try {
            val parts = value.split(':', limit = 2)
            when (parts.size) {
                1 -> ResourceLocation("minecraft", parts[0])
                2 -> ResourceLocation(parts[0], parts[1])
                else -> null
            }
        } catch (ex: Exception) {
            null
        }
    }
}
