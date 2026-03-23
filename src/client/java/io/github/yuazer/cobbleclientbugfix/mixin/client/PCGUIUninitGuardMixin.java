package io.github.yuazer.cobbleclientbugfix.mixin.client;

import com.cobblemon.mod.common.client.gui.pc.PCGUI;
import com.cobblemon.mod.common.client.gui.pc.StorageWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Prevents crash when interacting with PCGUI before storageWidget is initialized.
 * Cobblemon creates NavigationButtons referencing storageWidget before it is assigned
 * in init(), so early clicks/keys/scrolls hit an uninitialized lateinit property.
 */
@Mixin(value = PCGUI.class, remap = false)
public abstract class PCGUIUninitGuardMixin {

    @Shadow
    private StorageWidget storageWidget;

    @Inject(
        method = "mouseClicked(DDI)Z",
        at = @At("HEAD"),
        cancellable = true,
        remap = true
    )
    private void cobblebugfix_guardMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (storageWidget == null) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
        method = "mouseScrolled(DDDD)Z",
        at = @At("HEAD"),
        cancellable = true,
        remap = true
    )
    private void cobblebugfix_guardMouseScrolled(double mouseX, double mouseY, double amount, double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
        if (storageWidget == null) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
        method = "keyPressed(III)Z",
        at = @At("HEAD"),
        cancellable = true,
        remap = true
    )
    private void cobblebugfix_guardKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (storageWidget == null) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
        method = "mouseDragged(DDIDD)Z",
        at = @At("HEAD"),
        cancellable = true,
        remap = true
    )
    private void cobblebugfix_guardMouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY, CallbackInfoReturnable<Boolean> cir) {
        if (storageWidget == null) {
            cir.setReturnValue(false);
        }
    }

}
