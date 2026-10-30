package com.zrlog.plugin.mail.service;

import com.zrlog.plugin.IMsgPacketCallBack;
import com.zrlog.plugin.data.codec.MsgPacket;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Owns the asynchronous configuration callback until delivery and its cleanup have actually returned. */
final class PendingDelivery implements IMsgPacketCallBack {
    private final IMsgPacketCallBack delivery;
    private final CountDownLatch finished = new CountDownLatch(1);
    private Thread worker;
    private boolean cancelled;
    private Throwable failure;

    PendingDelivery(IMsgPacketCallBack delivery) { this.delivery = delivery; }

    @Override public void handler(MsgPacket response) {
        synchronized (this) {
            if (cancelled) return;
            worker = Thread.currentThread();
        }
        try {
            delivery.handler(response);
        } catch (RuntimeException | Error error) {
            failure = error;
        } finally {
            synchronized (this) {
                worker = null;
                finished.countDown();
            }
        }
    }

    void awaitCompletion() {
        try {
            if (!finished.await(60, TimeUnit.SECONDS)) {
                cancelAndAwait();
                throw new IllegalStateException("Email delivery timed out");
            }
        } catch (InterruptedException interrupted) {
            cancelAndAwait();
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Email delivery cancelled", interrupted);
        }
        if (failure != null) throw new IllegalStateException("Email delivery failed", failure);
    }

    private void cancelAndAwait() {
        synchronized (this) {
            cancelled = true;
            if (worker == null) finished.countDown(); // A late configuration reply must not start delivery.
            else worker.interrupt();
        }
        boolean interrupted = false;
        while (true) {
            try { finished.await(); break; }
            catch (InterruptedException ignored) { interrupted = true; }
        }
        if (interrupted) Thread.currentThread().interrupt();
    }
}
