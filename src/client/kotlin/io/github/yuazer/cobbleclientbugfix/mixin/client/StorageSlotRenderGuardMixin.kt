package io.github.yuazer.cobbleclientbugfix.mixin.client

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.gui.blitk
import com.cobblemon.mod.common.client.gui.PokemonGUIAnimationStyle
import com.cobblemon.mod.common.client.gui.drawProfilePokemon
import com.cobblemon.mod.common.client.gui.pasture.PasturePCGUIConfiguration
import com.cobblemon.mod.common.client.gui.pc.PCGUI
import com.cobblemon.mod.common.client.gui.pc.PartyStorageSlot
import com.cobblemon.mod.common.client.gui.pc.StorageSlot
import com.cobblemon.mod.common.client.gui.pc.StorageWidget
import com.cobblemon.mod.common.client.render.drawScaledText
import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState
import com.cobblemon.mod.common.client.render.renderScaledGuiItemIcon
import com.cobblemon.mod.common.pokemon.Gender
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.util.cobblemonResource
import com.cobblemon.mod.common.util.lang
import com.cobblemon.mod.common.util.math.fromEulerXYZDegrees
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import org.joml.Quaternionf
import org.joml.Vector3f
import org.slf4j.LoggerFactory
import org.spongepowered.asm.mixin.Final
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Overwrite
import org.spongepowered.asm.mixin.Shadow

@Mixin(value = [StorageSlot::class], remap = false)
abstract class StorageSlotRenderGuardMixin {
    @Shadow
    @Final
    private lateinit var parent: StorageWidget

    @Shadow
    @Final
    private lateinit var state: FloatingState

    @Shadow
    protected var isSlotSelected: Boolean = false

    @Shadow
    abstract fun isStationary(): Boolean

    @Shadow
    abstract fun getPokemon(): Pokemon?

    @Shadow
    abstract fun isHoveredOrFocused(): Boolean

    /**
     * Prevent client crash when third-party model data breaks Cobblemon profile rendering in PC slots.
     */
    @Overwrite(remap = false)
    fun renderSlot(context: GuiGraphics, posX: Int, posY: Int, partialTicks: Float) {
        val pokemon = getPokemon() ?: return
        val matrices = context.pose()
        context.enableScissor(
            posX - 2,
            posY + 2,
            posX + StorageSlot.SIZE + 4,
            posY + StorageSlot.SIZE + 4
        )

        matrices.pushPose()
        matrices.translate(posX + (StorageSlot.SIZE / 2.0), posY + 1.0, 0.0)
        matrices.scale(2.5F, 2.5F, 1F)

        val animationConfig = if (this is PartyStorageSlot) {
            Cobblemon.config.summaryProfileAnimations
        } else {
            Cobblemon.config.pcProfileAnimations
        }
        val shouldAnimate = when (animationConfig) {
            PokemonGUIAnimationStyle.ALWAYS_ANIMATE -> true
            PokemonGUIAnimationStyle.NEVER_ANIMATE -> false
            PokemonGUIAnimationStyle.ANIMATE_SELECTED -> isHoveredOrFocused()
        }

        try {
            drawProfilePokemon(
                renderablePokemon = pokemon.asRenderablePokemon(),
                matrixStack = matrices,
                rotation = Quaternionf().fromEulerXYZDegrees(Vector3f(13F, 35F, 0F)),
                state = state,
                partialTicks = if (shouldAnimate) partialTicks else 0F,
                scale = 4.5F
            )
        } catch (t: Throwable) {
            LOGGER.error(
                "Prevented Cobblemon PC render crash in StorageSlot: species={}, slot=({}, {}), pokemon={}",
                pokemon.species.resourceIdentifier,
                posX,
                posY,
                pokemon.uuid,
                t
            )
        }

        matrices.popPose()
        context.disableScissor()

        if (!isSlotSelected) {
            matrices.pushPose()
            matrices.translate(0.0, 0.0, 100.0)

            drawScaledText(
                context = context,
                text = lang("ui.lv.number", pokemon.level),
                x = posX + 1,
                y = posY + 1,
                shadow = true,
                scale = PCGUI.SCALE
            )

            if (pokemon.gender != Gender.GENDERLESS) {
                blitk(
                    matrixStack = matrices,
                    texture = if (pokemon.gender == Gender.MALE) GENDER_ICON_MALE else GENDER_ICON_FEMALE,
                    x = (posX + 21) / PCGUI.SCALE,
                    y = (posY + 1) / PCGUI.SCALE,
                    width = 6,
                    height = 8,
                    scale = PCGUI.SCALE
                )
            }

            val heldItem = pokemon.heldItem().copy()
            if (!heldItem.isEmpty) {
                renderScaledGuiItemIcon(
                    itemStack = heldItem,
                    x = posX + 16.0,
                    y = posY + 16.0,
                    scale = 0.5,
                    matrixStack = matrices
                )
            }
            matrices.popPose()
        }

        matrices.pushPose()
        matrices.translate(0.0, 0.0, 500.0)

        val config = parent.pcGui.configuration
        if (pokemon.tetheringId != null && !isSlotSelected) {
            if (isStationary()) {
                blitk(
                    matrixStack = matrices,
                    x = posX,
                    y = posY,
                    width = StorageSlot.SIZE,
                    height = StorageSlot.SIZE,
                    texture = SLOT_OVERLAY_RESOURCE
                )
            }

            val opacity = if (
                config is PasturePCGUIConfiguration &&
                config.pasturedPokemon.get().none { it.pokemonId == pokemon.uuid }
            ) {
                0.5F
            } else {
                1F
            }

            blitk(
                matrixStack = matrices,
                x = (posX + 7.5) / PCGUI.SCALE,
                y = (posY + 7.5) / PCGUI.SCALE,
                width = 20,
                height = 20,
                texture = SLOT_OVERLAY_PASTURE_ICON_RESOURCE,
                scale = PCGUI.SCALE,
                alpha = opacity
            )
        }

        if (isHoveredOrFocused()) {
            if (
                config is PasturePCGUIConfiguration &&
                pokemon.tetheringId == null &&
                isStationary() &&
                config.permissions.canPasture &&
                config.canSelect(pokemon) &&
                config.pasturedPokemon.get().size < config.limit &&
                config.pasturedPokemon.get().count { it.playerId == Minecraft.getInstance().player!!.uuid } < config.permissions.maxPokemon
            ) {
                blitk(
                    matrixStack = matrices,
                    x = posX,
                    y = posY,
                    width = StorageSlot.SIZE,
                    height = StorageSlot.SIZE,
                    texture = SLOT_OVERLAY_RESOURCE
                )

                blitk(
                    matrixStack = matrices,
                    x = (posX + 7.5) / PCGUI.SCALE,
                    y = (posY + 7.5) / PCGUI.SCALE,
                    width = 20,
                    height = 20,
                    texture = SLOT_OVERLAY_MOVE_ICON_RESOURCE,
                    scale = PCGUI.SCALE
                )
            }

            blitk(
                matrixStack = matrices,
                texture = SELECT_POINTER_RESOURCE,
                x = (posX + 10) / PCGUI.SCALE,
                y = ((posY - 3) / PCGUI.SCALE) - parent.pcGui.selectPointerOffsetY,
                width = 11,
                height = 8,
                scale = PCGUI.SCALE
            )
        }
        matrices.popPose()
    }

    private companion object {
        private val LOGGER = LoggerFactory.getLogger("CobbleBugFix/StorageSlotRenderGuard")
        private val GENDER_ICON_MALE = cobblemonResource("textures/gui/pc/gender_icon_male.png")
        private val GENDER_ICON_FEMALE = cobblemonResource("textures/gui/pc/gender_icon_female.png")
        private val SELECT_POINTER_RESOURCE = cobblemonResource("textures/gui/pc/pc_pointer.png")
        private val SLOT_OVERLAY_RESOURCE = cobblemonResource("textures/gui/pc/pc_slot_overlay.png")
        private val SLOT_OVERLAY_PASTURE_ICON_RESOURCE = cobblemonResource("textures/gui/pasture/pc_slot_icon_pasture.png")
        private val SLOT_OVERLAY_MOVE_ICON_RESOURCE = cobblemonResource("textures/gui/pasture/pc_slot_icon_move.png")
    }
}
