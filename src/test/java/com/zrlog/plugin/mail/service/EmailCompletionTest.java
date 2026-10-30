package com.zrlog.plugin.mail.service;

import com.google.gson.Gson;
import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.client.ClientActionHandler;
import com.zrlog.plugin.data.codec.*;
import com.zrlog.plugin.message.*;
import com.zrlog.plugin.support.MemorySocketChannels;
import com.zrlog.plugin.type.ActionType;
import org.junit.Test;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;

public class EmailCompletionTest {
    @Test public void asynchronousCallbackAndAuditFinishBeforeTheCapabilityReply() throws Exception {
        try (Harness h = new Harness()) {
            Future<?> invocation = h.invoke();
            assertTrue(h.configRequested.await(5, TimeUnit.SECONDS));
            assertFalse(invocation.isDone());
            assertTrue(h.responses.isEmpty());
            Future<?> callback = h.replyConfig();
            assertTrue(h.recordStarted.await(5, TimeUnit.SECONDS));
            assertFalse(invocation.isDone());
            assertTrue(h.responses.isEmpty());
            h.finishRecord.countDown();
            callback.get(5, TimeUnit.SECONDS); invocation.get(5, TimeUnit.SECONDS);
            CapabilityInvokeResult result = h.result();
            assertTrue(result.isExecutionCompleted());
            assertFalse(result.isSuccess());
            assertFalse(result.isOutcomeUncertain());
            assertEquals("missing title or content", result.getErrorMessage());
            assertTrue(h.savedRecord.contains("missing title or content"));
        }
    }

    @Test public void cancellationBeforeConfigurationPreventsLateDelivery() throws Exception {
        try (Harness h = new Harness()) {
            Future<?> invocation = h.invoke();
            assertTrue(h.configRequested.await(5, TimeUnit.SECONDS));
            h.cancel(); invocation.get(5, TimeUnit.SECONDS);
            assertTrue(h.result().isExecutionCompleted());
            assertTrue(h.result().isOutcomeUncertain());
            h.replyConfig().get(5, TimeUnit.SECONDS);
            assertNull(h.savedRecord);
            assertEquals(2, h.responses.size()); // Completion and cancellation receipts only.
        }
    }

    @Test public void cancellationWaitsForCallbackCleanupBeforeAcknowledgingCompletion() throws Exception {
        try (Harness h = new Harness()) {
            Future<?> invocation = h.invoke();
            assertTrue(h.configRequested.await(5, TimeUnit.SECONDS));
            Future<?> callback = h.replyConfig();
            assertTrue(h.recordStarted.await(5, TimeUnit.SECONDS));
            h.cancel();
            assertTrue(h.recordInterrupted.await(5, TimeUnit.SECONDS));
            assertTrue(h.responses.isEmpty());
            assertFalse(invocation.isDone());
            h.finishRecord.countDown();
            callback.get(5, TimeUnit.SECONDS); invocation.get(5, TimeUnit.SECONDS);
            assertTrue(h.result().isExecutionCompleted());
            assertTrue(h.result().isOutcomeUncertain());
            assertFalse(h.result().isSuccess());
            assertEquals(2, h.responses.size());
        }
    }

    private static class Harness implements AutoCloseable {
        final CountDownLatch configRequested = new CountDownLatch(1), recordStarted = new CountDownLatch(1),
                finishRecord = new CountDownLatch(1), recordInterrupted = new CountDownLatch(1);
        final List<MsgPacket> responses = new CopyOnWriteArrayList<>();
        final ExecutorService workers = Executors.newFixedThreadPool(2);
        volatile MsgPacket configRequest;
        volatile String savedRecord;
        final IOSession session;
        Harness() {
            session = new IOSession(new MemorySocketChannels.MemoryChannel(), new MemorySocketChannels.MemorySelector(),
                    new SocketCodec(new SocketEncode() {
                        @Override public void doEncode(IOSession target, MsgPacket packet) {
                            if (packet.getStatus() != MsgPacketStatus.SEND_REQUEST) { responses.add(packet); return; }
                            if (ActionType.GET_WEBSITE.name().equals(packet.getMethodStr()) && configRequest == null) {
                                configRequest = packet; configRequested.countDown(); return;
                            }
                            if (ActionType.SET_WEBSITE.name().equals(packet.getMethodStr())) {
                                savedRecord = packet.getDataStr(); recordStarted.countDown();
                                boolean interrupted = false;
                                while (true) {
                                    try { finishRecord.await(); break; }
                                    catch (InterruptedException ignored) { interrupted = true; recordInterrupted.countDown(); }
                                }
                                if (interrupted) Thread.currentThread().interrupt();
                            }
                            reply(target, packet);
                        }
                    }, new SocketDecode(Runnable::run)), new ClientActionHandler());
            session.getAttr().put("_pluginServices", Collections.singletonList(EmailService.class));
        }
        Future<?> invoke() {
            CapabilityInvokeRequest request = new CapabilityInvokeRequest();
            request.setCapabilityKey("notification.email.send"); request.setRequestId("mail-fixture");
            request.setRequireCompletion(true); request.setPayload(Collections.emptyMap());
            return workers.submit(() -> session.dispatchIfOpen(new MsgPacket(request, ContentType.JSON,
                    MsgPacketStatus.SEND_REQUEST, 1, ActionType.CAPABILITY_INVOKE.name()), () -> { }));
        }
        Future<?> replyConfig() { return workers.submit(() -> reply(session, configRequest)); }
        void cancel() {
            CapabilityCancelRequest request = new CapabilityCancelRequest();
            request.setInvocationMessageId(1); request.setRequestId("mail-fixture");
            session.dispatchIfOpen(new MsgPacket(request, ContentType.JSON, MsgPacketStatus.SEND_REQUEST, 2,
                    ActionType.CAPABILITY_CANCEL.name()), () -> { });
        }
        static void reply(IOSession target, MsgPacket request) {
            target.dispose(new MsgPacket(Collections.emptyMap(), ContentType.JSON, MsgPacketStatus.RESPONSE_SUCCESS,
                    request.getMsgId(), request.getMethodStr()));
        }
        CapabilityInvokeResult result() {
            assertFalse(responses.isEmpty());
            return new Gson().fromJson(responses.get(0).getDataStr(), CapabilityInvokeResult.class);
        }
        public void close() throws Exception {
            finishRecord.countDown(); workers.shutdownNow();
            assertTrue(workers.awaitTermination(5, TimeUnit.SECONDS)); session.close();
        }
    }
}
