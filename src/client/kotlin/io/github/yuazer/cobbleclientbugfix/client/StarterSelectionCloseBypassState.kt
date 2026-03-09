package io.github.yuazer.cobbleclientbugfix.client

object StarterSelectionCloseBypassState {
    @Volatile
    private var lastSelectStarterSendAt: Long = 0

    @JvmStatic
    fun markSelectStarterSent() {
        lastSelectStarterSendAt = System.currentTimeMillis()
    }

    @JvmStatic
    fun consumeIfRecent(now: Long, maxAgeMillis: Long = 3000): Boolean {
        val sentAt = lastSelectStarterSendAt
        if (sentAt <= 0 || now - sentAt > maxAgeMillis) {
            return false
        }
        lastSelectStarterSendAt = 0
        return true
    }
}
