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
 * Intercept corrupted FastUtil ObjectOpenHashSet iterator state before it NPEs.
 */
@SuppressWarnings({"rawtypes", "unchecked", "unused"})
@Mixin(
        targets = "it.unimi.dsi.fastutil.objects.ObjectOpenHashSet$SetIterator",
        remap = false,
        priority = 1100
)
public abstract class FastUtilObjectSetIteratorMixin {

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("CobbleBugFix");

    @Shadow(remap = false)
    protected int pos;

    @Shadow(remap = false)
    protected int c;

    @Shadow(remap = false)
    protected int last;

    @Shadow(remap = false)
    protected it.unimi.dsi.fastutil.objects.ObjectArrayList wrapped;

    @Inject(
            method = "next()Ljava/lang/Object;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void cobble$preventWrappedNPE(CallbackInfoReturnable<Object> cir) {
        if (c > 0 && pos < 0 && wrapped == null) {
            LOGGER.warn("[Z菌修复]拦截fastutil崩溃 (ObjectOpenHashSet$SetIterator.next: wrapped is null, pos={}, c={})", pos, c);
            c = 0;
            pos = -1;
            last = -1;
            throw new NoSuchElementException("Iterator exhausted (corrupted ObjectOpenHashSet state)");
        }
    }
}
