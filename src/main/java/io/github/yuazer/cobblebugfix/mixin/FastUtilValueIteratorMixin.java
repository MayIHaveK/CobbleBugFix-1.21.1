package io.github.yuazer.cobblebugfix.mixin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.util.NoSuchElementException;

/**
 * Guard against corrupted fastutil iterator state:
 * when c > 0, pos < 0 but wrapped is null -> wrapped.getInt(...) will NPE.
 */
@SuppressWarnings({"rawtypes", "unchecked", "unused"})
@Mixin(
        targets = "it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap$ValueIterator",
        remap = false
)
public abstract class FastUtilValueIteratorMixin {

    @Unique private static final Logger LOGGER = LoggerFactory.getLogger("CobbleBugFix");

    @Unique private static volatile boolean cobble$fieldsResolved = false;

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
                return;
            }

            int pos = cobble$fieldPos.getInt(this);
            int c = cobble$fieldC.getInt(this);
            Object wrapped = cobble$fieldWrapped.get(this);

            if (c > 0 && pos < 0 && wrapped == null) {
                cobble$fieldC.setInt(this, 0);
                if (cobble$fieldLast != null) {
                    cobble$fieldLast.setInt(this, -1);
                }

                LOGGER.warn("[Z菌修复]拦截fastutil崩溃 (ValueIterator.next: wrapped is null, pos={}, c={})", pos, c);
                throw new NoSuchElementException("Iterator exhausted (corrupted ValueIterator state)");
            }
        } catch (NoSuchElementException e) {
            throw e;
        } catch (Throwable ignored) {
            // Let original behavior continue.
        }
    }

    @Unique
    private static void cobble$resolveFieldsIfNeeded(Class<?> valueIteratorClass) {
        if (cobble$fieldsResolved) return;
        cobble$fieldsResolved = true;

        cobble$fieldPos = cobble$findFieldUpwards(valueIteratorClass, "pos");
        cobble$fieldC = cobble$findFieldUpwards(valueIteratorClass, "c");
        cobble$fieldLast = cobble$findFieldUpwards(valueIteratorClass, "last");
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
