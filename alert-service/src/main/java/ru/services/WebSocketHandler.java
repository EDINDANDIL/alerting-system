package ru.services;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.kora.common.Component;

@Component
public final class WebSocketHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    private static final Logger log = LoggerFactory.getLogger(WebSocketHandler.class);
    private static final AttributeKey<Integer> USER_ID_ATTR = AttributeKey.valueOf("userId");

    private final ConnectionManager connectionManager;

    public WebSocketHandler(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete complete) {
            int userId = extractUserId(complete.requestUri());
            if (userId <= 0) {
                log.warn("Rejecting ws connection without valid userId, uri={}", complete.requestUri());
                ctx.close();
                return;
            }
            ctx.channel().attr(USER_ID_ATTR).set(userId);
            connectionManager.register(userId, ctx.channel());
            log.info("WebSocket handshake completed for user {}", userId);
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
        String message = frame.text();
        if ("ping".equalsIgnoreCase(message)) {
            ctx.writeAndFlush(new TextWebSocketFrame("pong"));
            return;
        }
        log.debug("WS inbound message ignored: {}", message);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        connectionManager.unregister(ctx.channel());
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Integer userId = ctx.channel().attr(USER_ID_ATTR).get();
        log.warn("WebSocket error for user {}", userId, cause);
        ctx.close();
    }

    static int extractUserId(String uri) {
        try {
            int queryIndex = uri.indexOf('?');
            if (queryIndex < 0 || queryIndex + 1 >= uri.length()) {
                return -1;
            }
            String query = uri.substring(queryIndex + 1);
            String[] params = query.split("&");
            for (String param : params) {
                String[] pair = param.split("=");
                if (pair.length == 2 && "userId".equals(pair[0])) {
                    return Integer.parseInt(pair[1]);
                }
            }
        } catch (Exception ignored) {
            return -1;
        }
        return -1;
    }
}
