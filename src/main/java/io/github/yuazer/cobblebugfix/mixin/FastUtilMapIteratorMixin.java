package io.github.yuazer.cobblebugfix.mixin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.NoSuchElementException;

/**
 * Guard against corrupted fastutil iterator state:
 * c > 0, pos < 0, wrapped == null.
 */
@SuppressWarnings({"rawtypes", "unchecked", "unused"})
@Mixin(
        targets = "it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap$MapIterator",
        remap = false,
        priority = 1100
)
public abstract class FastUtilMapIteratorMixin {

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("CobbleBugFix");

    @Shadow(remap = false)
    protected int pos;

    @Shadow(remap = false)
    protected int c;

    @Shadow(remap = false)
    protected it.unimi.dsi.fastutil.ints.IntArrayList wrapped;

    @Unique
    private boolean cobble$isBrokenState() {
        return c > 0 && pos < 0 && wrapped == null;
    }

    @Inject(method = "hasNext()Z", at = @At("HEAD"), cancellable = true, remap = false)
    private void cobble$fixHasNextWhenWrappedNull(CallbackInfoReturnable<Boolean> cir) {
        if (cobble$isBrokenState()) {
            LOGGER.warn("[CobbleBugFix] fastutil MapIterator corrupted state, forcing hasNext=false (pos={}, c={})", pos, c);
            c = 0;
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "nextEntry()I", at = @At("HEAD"), cancellable = true, remap = false)
    private void cobble$preventWrappedNPE(CallbackInfoReturnable<Integer> cir) {
        if (cobble$isBrokenState()) {
            LOGGER.warn("[CobbleBugFix] fastutil MapIterator corrupted state, preventing nextEntry NPE (pos={}, c={})", pos, c);
            c = 0;
            throw new NoSuchElementException("Iterator exhausted (corrupted state detected)");
        }
    }
}
