package io.github.yuazer.cobbleclientbugfix.client

import io.github.yuazer.cobblebugfix.network.ModelSourcePackets
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import org.slf4j.LoggerFactory

class CobbleclientbugfixClient : ClientModInitializer {
    override fun onInitializeClient() {
        ModelSourcePackets.registerPayloadTypes()

        ClientPlayNetworking.registerGlobalReceiver(ModelSourcePackets.ModelSourceRequestPayload.TYPE) { payload, _ ->
            val result = LocalModelSourceTracker.getSource(payload.speciesId)
            ClientPlayNetworking.send(
                ModelSourcePackets.ModelSourceResponsePayload(
                    speciesId = payload.speciesId,
                    jsonResource = result?.jsonResource?.toString() ?: "unknown",
                    sourcePack = result?.sourcePack ?: "unknown",
                    localPath = result?.localPath ?: "unknown"
                )
            )
        }

        LoggerFactory.getLogger("CobbleBugFix").info("CobbleBugFix client initialized")
    }
}
