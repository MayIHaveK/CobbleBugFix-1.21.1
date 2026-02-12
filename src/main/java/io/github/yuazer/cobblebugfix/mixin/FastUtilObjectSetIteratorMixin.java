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
 * 拦截 FastUtil ObjectOpenHashSet$SetIterator.next 中的 NPE 崩溃
 * 当 wrapped 为 null 但仍尝试访问时，抛出安全异常而不是 NPE
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
    protected it.unimi.dsi.fastutil.objects.ObjectArrayList wrapped;

    /**
     * 在 next 方法开始时检查 wrapped 状态
     * 如果 wrapped 为 null 但 pos < 0，说明迭代器状态损坏，会导致 NPE
     */
    @Inject(
            method = "next()Ljava/lang/Object;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void cobble$preventWrappedNPE(CallbackInfoReturnable<Object> cir) {
        try {
            // 检查是否会在访问 wrapped 时发生 NPE
            // 当 pos < 0 时，next 会尝试访问 wrapped.get(...)
            if (pos < 0 && wrapped == null) {
                LOGGER.warn("[Z菌修复]拦截fastutil崩溃 (ObjectOpenHashSet$SetIterator.next: wrapped is null, pos={}, c={})", pos, c);

                // 重置迭代器状态，防止后续调用继续出错
                c = 0;
                pos = -1;

                // 返回 null 而不是抛出异常，避免崩服
                cir.setReturnValue(null);
                cir.cancel();
            }
        } catch (Throwable t) {
            // 如果检查本身出错，记录但不影响原有逻辑
            LOGGER.debug("[Z菌修复]FastUtil ObjectSet 状态检查失败", t);
        }
    }
}
