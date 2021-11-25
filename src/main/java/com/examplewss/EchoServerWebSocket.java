package com.examplewss;

import io.micronaut.http.annotation.Header;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.websocket.CloseReason;
import io.micronaut.websocket.WebSocketBroadcaster;
import io.micronaut.websocket.WebSocketSession;
import io.micronaut.websocket.annotation.OnClose;
import io.micronaut.websocket.annotation.OnMessage;
import io.micronaut.websocket.annotation.OnOpen;
import io.micronaut.websocket.annotation.ServerWebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micronaut.core.annotation.Nullable;
import java.util.function.Predicate;

@Secured(SecurityRule.IS_ANONYMOUS)
@ServerWebSocket("/v1/{number}/{uuid}/{id}")
public class EchoServerWebSocket {
    protected static final Logger LOG = LoggerFactory.getLogger(EchoServerWebSocket.class);

    private WebSocketBroadcaster broadcaster;

    public EchoServerWebSocket(WebSocketBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @OnOpen
    public void onOpen(WebSocketSession session, @Nullable @Header("Authorization") String authorization) {
        String msg = "{\"message\": \"Connection - OK\"}";
        session.put("CFG_MSG_NOT_EXIST", true);
        broadcaster.broadcastSync(msg, isValid(session));
    }

    @OnMessage
    public void onMessage(String message, WebSocketSession session) {
        if (session.get("CFG_MSG", Boolean.class).orElse(true)) {
            LOG.info("Got config message: {}", message);

            session.put("CFG_MSG", false);

            String msg = "{\"message\": \"Config Frame - OK\"}";
            broadcaster.broadcastSync(msg, isValid(session));
        } else {
            session.close(CloseReason.UNSUPPORTED_DATA);
        }
    }

    @OnClose
    public void onClose(WebSocketSession session) {
        LOG.info("Closed connection");
    }

    private Predicate<WebSocketSession> isValid(WebSocketSession session) {
        return s -> s.equals(session);
    }
}