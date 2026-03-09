package io.github.yuazer.cobblebugfix.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerEntity.class)
public abstract class ServerEntitySendChangesCrashGuardMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger("CrashGuard/ServerEntity");
    private static final long WARN_INTERVAL_MS = 5000L;
    private static long cobble$lastWarnMs = 0L;
    private static int cobble$suppressedWarns = 0;

    @WrapMethod(method = "sendChanges")
    private void crashGuard$sendChanges(Operation<Void> original) {
        try {
            original.call();
        } catch (Throwable t) {
            if (cobble$isFastutilIteratorCorruption(t)) {
                cobble$logRateLimited(t);
                return;
            }
            throw t;
        }
    }

    private static boolean cobble$isFastutilIteratorCorruption(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cobble$isFastutilEntitySyncCrashStack(cur.getStackTrace())) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean cobble$isFastutilEntitySyncCrashStack(StackTraceElement[] stack) {
        boolean hasFastutilIteratorFrame = false;
        boolean hasServerEntityFrame = false;
        boolean hasEntityDataPacketCtorFrame = false;

        for (StackTraceElement ste : stack) {
            String owner = ste.getClassName();
            String method = ste.getMethodName();
            if (owner == null) {
                continue;
            }

            if (owner.startsWith("it.unimi.dsi.fastutil.")
                    && (owner.contains("ObjectOpenHashSet$SetIterator")
                    || owner.contains("Int2ObjectOpenHashMap$MapIterator")
                    || owner.contains("Int2ObjectOpenHashMap$ValueIterator"))
                    && ("next".equals(method) || "nextEntry".equals(method))) {
                hasFastutilIteratorFrame = true;
            }

            if (owner.equals("net.minecraft.class_3231")
                    || owner.equals("net.minecraft.server.level.ServerEntity")) {
                hasServerEntityFrame = true;
            }

            if ((owner.equals("net.minecraft.class_2781") && "<init>".equals(method))
                    || owner.equals("net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket")) {
                hasEntityDataPacketCtorFrame = true;
            }
        }

        return hasFastutilIteratorFrame && hasServerEntityFrame && hasEntityDataPacketCtorFrame;
    }

    private static void cobble$logRateLimited(Throwable t) {
        long now = System.currentTimeMillis();
        if (now - cobble$lastWarnMs >= WARN_INTERVAL_MS) {
            int suppressed = cobble$suppressedWarns;
            cobble$suppressedWarns = 0;
            cobble$lastWarnMs = now;
            if (suppressed > 0) {
                LOGGER.warn("[Z菌修复]拦截一次fastutil并发修改导致崩服 ({} 次重复日志已折叠)", suppressed, t);
            } else {
                LOGGER.warn("[Z菌修复]拦截一次fastutil并发修改导致崩服", t);
            }
        } else {
            cobble$suppressedWarns++;
        }
    }
}
