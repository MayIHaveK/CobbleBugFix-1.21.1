package io.github.yuazer.cobblebugfix.mixin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;

/**
 * Guard against corrupted fastutil iterator state:
 * when c > 0, pos < 0 but wrapped is null -> wrapped.getInt(...) will NPE.
 *
 * We avoid @Shadow because the iterator fields are declared in the parent
 * class (MapIterator), and annotation processor may fail to resolve them
 * when mixing into an inner class via targets="...$ValueIterator".
 */
@SuppressWarnings({"rawtypes", "unchecked", "unused"})
@Mixin(
        targets = "it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap$ValueIterator",
        remap = false
)
public abstract class FastUtilValueIteratorMixin {

    @Unique private static final Logger LOGGER = LoggerFactory.getLogger("CobbleBugFix");

    @Unique private static volatile boolean cobble$fieldsResolved = false;

    // These are in Int2ObjectOpenHashMap$MapIterator (superclass of ValueIterator)
    @Unique private static Field cobble$fieldPos;
    @Unique private static Field cobble$fieldC;
    @Unique private static Field cobble$fieldLast;
    @Unique private static Field cobble$fieldWrapped;

    @Inject(
            method = "next()Ljava/lang/Object;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void cobble$guardBrokenIterator(CallbackInfoReturnable<Object> cir) {
        try {
            cobble$resolveFieldsIfNeeded(this.getClass());

            if (cobble$fieldPos == null || cobble$fieldC == null || cobble$fieldWrapped == null) {
                // If we can't resolve the minimal required fields, do nothing.
                return;
            }

            int pos = cobble$fieldPos.getInt(this);
            int c = cobble$fieldC.getInt(this);
            Object wrapped = cobble$fieldWrapped.get(this);

            // If iterator says it still has elements, but it already underflowed pos (needs wrapped),
            // and wrapped is null -> nextEntry will NPE.
            if (c > 0 && pos < 0 && wrapped == null) {
                // Mark exhausted to keep hasNext() consistent
                cobble$fieldC.setInt(this, 0);

                // Best-effort: reset last so remove() won't act on stale state
                if (cobble$fieldLast != null) {
                    cobble$fieldLast.setInt(this, -1);
                }

                LOGGER.info("[Z菌修复]已拦截一次 fastutil ValueIterator 崩溃 (pos<0 && wrapped==null && c>0)");
                cir.setReturnValue(null);
            }
        } catch (Throwable ignored) {
            // If anything goes wrong, let original behavior continue.
        }
    }

    @Unique
    private static void cobble$resolveFieldsIfNeeded(Class<?> valueIteratorClass) {
        if (cobble$fieldsResolved) return;
        cobble$fieldsResolved = true;

        // Walk up the class hierarchy to find fields (they live in MapIterator)
        cobble$fieldPos = cobble$findFieldUpwards(valueIteratorClass, "pos");
        cobble$fieldC = cobble$findFieldUpwards(valueIteratorClass, "c");

        // last may not exist in some variants; optional
        cobble$fieldLast = cobble$findFieldUpwards(valueIteratorClass, "last");

        // wrapped might be renamed in some builds; try common candidates
        cobble$fieldWrapped = cobble$findFieldUpwards(valueIteratorClass,
                "wrapped",
                "wrap",
                "wrappedKeys",
                "wrappedList"
        );
    }

    @Unique
    private static Field cobble$findFieldUpwards(Class<?> start, String... names) {
        Class<?> c = start;
        while (c != null) {
            for (String name : names) {
                try {
                    Field f = c.getDeclaredField(name);
                    f.setAccessible(true);
                    return f;
                } catch (NoSuchFieldException ignored) {
                } catch (Throwable t) {
                    return null;
                }
            }
            c = c.getSuperclass();
        }
        return null;
    }
}
