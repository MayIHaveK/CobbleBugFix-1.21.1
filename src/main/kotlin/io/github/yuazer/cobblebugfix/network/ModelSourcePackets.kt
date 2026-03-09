package io.github.yuazer.cobblebugfix.network

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

object ModelSourcePackets {
    private val REQUEST_ID = ResourceLocation("cobblebugfix", "client_model_source_request")
    private val RESPONSE_ID = ResourceLocation("cobblebugfix", "client_model_source_response")

    @Volatile
    private var registered = false

    @JvmStatic
    fun registerPayloadTypes() {
        if (registered) {
            return
        }
        synchronized(this) {
            if (registered) {
                return
            }
            PayloadTypeRegistry.playS2C().register(ModelSourceRequestPayload.TYPE, ModelSourceRequestPayload.CODEC)
            PayloadTypeRegistry.playC2S().register(ModelSourceResponsePayload.TYPE, ModelSourceResponsePayload.CODEC)
            registered = true
        }
    }

    data class ModelSourceRequestPayload(
        val speciesId: ResourceLocation
    ) : CustomPacketPayload {
        override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

        companion object {
            val TYPE = CustomPacketPayload.Type<ModelSourceRequestPayload>(REQUEST_ID)
            val CODEC: StreamCodec<RegistryFriendlyByteBuf, ModelSourceRequestPayload> = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC,
                ModelSourceRequestPayload::speciesId,
                ::ModelSourceRequestPayload
            )
        }
    }

    data class ModelSourceResponsePayload(
        val speciesId: ResourceLocation,
        val jsonResource: String,
        val sourcePack: String,
        val localPath: String
    ) : CustomPacketPayload {
        override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

        companion object {
            val TYPE = CustomPacketPayload.Type<ModelSourceResponsePayload>(RESPONSE_ID)
            val CODEC: StreamCodec<RegistryFriendlyByteBuf, ModelSourceResponsePayload> = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC,
                ModelSourceResponsePayload::speciesId,
                ByteBufCodecs.STRING_UTF8,
                ModelSourceResponsePayload::jsonResource,
                ByteBufCodecs.STRING_UTF8,
                ModelSourceResponsePayload::sourcePack,
                ByteBufCodecs.STRING_UTF8,
                ModelSourceResponsePayload::localPath,
                ::ModelSourceResponsePayload
            )
        }
    }
}

