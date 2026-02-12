package io.github.yuazer.cobblebugfix.mixin;

import io.netty.channel.ChannelFuture;
import net.minecraft.network.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Fix: prevent watchdog lockups caused by syncUninterruptibly() during login handshake.
 *
 * Strategy:
 *  Replace infinite blocking with a short await, then continue to avoid deadlocking the
 *  server thread. This is a safety stop; it does not fix the root cause.
 */
@Mixin(Connection.class)
public abstract class ConnectionSyncUninterruptiblyMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger("CobbleBugFix");
    @Unique private static final AtomicLong COBBLE_LAST_LOG_MS = new AtomicLong(0);

    @Redirect(
            method = {
                    "syncAfterConfigurationChange",
                    "setupOutboundProtocol"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Lio/netty/channel/ChannelFuture;syncUninterruptibly()Lio/netty/channel/ChannelFuture;",
                    remap = false
            ),
            require = 0
    )
    private static ChannelFuture cobble$avoidInfiniteSync(ChannelFuture future) {
        boolean ok = future.awaitUninterruptibly(1000, TimeUnit.MILLISECONDS);
        if (!ok) {
            long now = System.currentTimeMillis();
            long last = COBBLE_LAST_LOG_MS.get();
            if (now - last > 5000 && COBBLE_LAST_LOG_MS.compareAndSet(last, now)) {
                LOGGER.warn("[Z菌修复]拦截一次线程阻塞崩溃");
            }
        }
        return future;
    }
}
