package org.apache.dubbo.proxy.worker;

import com.alibaba.fastjson.JSON;

import org.apache.dubbo.proxy.dao.ServiceDefinition;
import org.apache.dubbo.proxy.dao.ServiceMapping;
import org.apache.dubbo.proxy.metadata.MetadataCollector;
import org.apache.dubbo.proxy.service.GenericInvoke;
import org.apache.dubbo.proxy.utils.Constants;
import org.apache.dubbo.proxy.utils.Tool;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.util.CharsetUtil;
import org.apache.dubbo.metadata.definition.model.FullServiceDefinition;
import org.apache.dubbo.metadata.definition.model.MethodDefinition;
import org.apache.dubbo.metadata.identifier.MetadataIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;


public class RequestWorker implements Runnable {

    private ServiceDefinition serviceDefinition;
    private ChannelHandlerContext ctx;
    private HttpRequest msg;
    private Logger logger = LoggerFactory.getLogger(RequestWorker.class);

    private MetadataCollector metadataCollector;

    private ServiceMapping serviceMapping;


    public RequestWorker(ServiceDefinition serviceDefinition, ChannelHandlerContext ctx, HttpRequest msg,
                         MetadataCollector metadataCollector, ServiceMapping serviceMapping) {
        this.serviceDefinition = serviceDefinition;
        this.ctx = ctx;
        this.msg = msg;
        this.serviceMapping = serviceMapping;
        this.metadataCollector = metadataCollector;
    }

    @Override
    public void run() {
        String serviceID = serviceDefinition.getServiceID();
        String interfaze = Tool.getInterface(serviceID);
        String group = Tool.getGroup(serviceID);
        String version = Tool.getVersion(serviceID);
        if (serviceDefinition.getParamTypes() == null && serviceDefinition.getParamValues() != null) {
            String[] types = getTypesFromMetadata(serviceDefinition.getApplication(), interfaze, group, version,
                    serviceDefinition.getMethodName(), serviceDefinition.getParamValues().length);
            serviceDefinition.setParamTypes(types);
        }
        Object result;
        try {
            result = GenericInvoke.genericCall(interfaze,group, version,
                    serviceDefinition);
        } catch (Exception e) {
            e.printStackTrace();
            result = e;
        }
        if (!writeResponse(ctx, result)) {
            ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

    private boolean writeResponse(ChannelHandlerContext ctx, Object result) {
        // Decide whether to close the connection or not.
        // Build the response object.
        boolean keepAlive = HttpUtil.isKeepAlive(this.msg);
        FullHttpResponse response = new DefaultFullHttpResponse(
                HTTP_1_1, OK,
                Unpooled.copiedBuffer(JSON.toJSONString(result), CharsetUtil.UTF_8));

        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");

        if (keepAlive) {
            // Add 'Content-Length' header only for a keep-alive connection.
            response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            // Add keep alive header as per:
            // - http://www.w3.org/Protocols/HTTP/1.1/draft-ietf-http-v11-spec-01.html#Connection
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }

//         Encode the cookie.
        String cookieString = msg.headers().get(HttpHeaderNames.COOKIE);
        if (cookieString != null) {
            Set<Cookie> cookies = ServerCookieDecoder.STRICT.decode(cookieString);
            if (!cookies.isEmpty()) {
                // Reset the cookies if necessary.
                for (Cookie cookie : cookies) {
                    response.headers().add(HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.STRICT.encode(cookie));
                }
            }
        }

        // Write the response.
        ctx.writeAndFlush(response);

        return keepAlive;
    }

    private String[] getTypesFromMetadata(String application, String interfaze, String group, String version, String methodName, int paramLen) {
        MetadataIdentifier identifier = new MetadataIdentifier(interfaze, version, group, Constants.PROVIDER_SIDE, application);
        String metadata = metadataCollector.getProviderMetaData(identifier);
        FullServiceDefinition serviceDefinition = JSON.parseObject(metadata, FullServiceDefinition.class);
        List<MethodDefinition> methods = serviceDefinition.getMethods();
        if (methods != null) {
            for (MethodDefinition m : methods) {
                if (Tool.sameMethod(m, methodName, paramLen)) {
                    return m.getParameterTypes();
                }
            }
        }
        return null;
    }
}
