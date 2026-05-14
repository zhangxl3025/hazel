package com.zxl.hazel.web.netty;

import com.zxl.hazel.route.RouteInfo;
import com.zxl.hazel.route.RouteRegistrar;
import com.zxl.hazel.util.JsonUtils;
import com.zxl.hazel.util.RouteMatcher;
import com.zxl.hazel.util.ArgumentResolver;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class NettyDispatcherHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final Logger log = LoggerFactory.getLogger(NettyDispatcherHandler.class);
    private final List<RouteInfo> routes;

    public NettyDispatcherHandler() {
        RouteRegistrar.registerRoutes();
        this.routes = RouteRegistrar.getRoutes();
        log.info("NettyDispatcherHandler initialized with {} routes", routes.size());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        String method = request.method().name();
        String uri = request.uri();
        String path = uri.split("\\?")[0];

        RouteInfo route = RouteMatcher.findRoute(routes, method, path);
        if (route == null) {
            sendError(ctx, NOT_FOUND, "No handler found for " + method + " " + path);
            return;
        }

        try {
            Map<String, String> pathParams = RouteMatcher.extractPathParams(route, path);
            Map<String, String> queryParams = RouteMatcher.parseQueryParams(uri);
            String body = request.content().toString(CharsetUtil.UTF_8);

            Object[] args = ArgumentResolver.prepareArgumentsForNetty(route.method, pathParams, queryParams, body);
            Object result = route.method.invoke(route.controller, args);
            sendResponse(ctx, OK, JsonUtils.toJson(result));
        } catch (Exception e) {
            log.error("Handler error", e);
            sendError(ctx, INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }


    private void sendResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String content) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HTTP_1_1, status, Unpooled.copiedBuffer(content, CharsetUtil.UTF_8)
        );
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status, String message) {
        String json = "{\"error\":\"" + (message != null ? message.replace("\"", "\\\"") : "Unknown") + "\"}";
        sendResponse(ctx, status, json);
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Netty handler error", cause);
        sendError(ctx, INTERNAL_SERVER_ERROR, cause.getMessage());
    }
}