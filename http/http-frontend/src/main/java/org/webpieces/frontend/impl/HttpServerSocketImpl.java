package org.webpieces.frontend.impl;

import com.webpieces.http2parser.api.Http2Parser;
import com.webpieces.http2parser.api.dto.Http2Settings;
import org.webpieces.data.api.DataWrapperGenerator;
import org.webpieces.data.api.DataWrapperGeneratorFactory;
import org.webpieces.frontend.api.HttpServerSocket;
import org.webpieces.httpcommon.api.Http2Engine;
import org.webpieces.httpcommon.api.Http2EngineFactory;
import org.webpieces.httpcommon.api.ResponseSender;
import org.webpieces.nio.api.channels.Channel;
import org.webpieces.nio.api.handlers.DataListener;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.webpieces.httpcommon.api.Http2Engine.HttpSide.SERVER;


public class HttpServerSocketImpl implements HttpServerSocket {
    private Channel channel;
    private DataListener dataListener;
    private ResponseSender responseSender;
    private Http2Parser http2Parser;
    private TimedListener timedListener;
    private DataWrapperGenerator dataGen = DataWrapperGeneratorFactory.createDataWrapperGenerator();

    public HttpServerSocketImpl(Channel channel, DataListener http11DataListener, ResponseSender http11ResponseSender,
                                Http2Parser http2Parser,
                                TimedListener timedListener) {
        this.channel = channel;
        this.dataListener = http11DataListener;
        this.responseSender = http11ResponseSender;
        this.http2Parser = http2Parser;
        this.timedListener = timedListener;
    }

    @Override
    public synchronized void upgradeHttp2(Optional<ByteBuffer> maybeSettingsFrame) {
        Http2Engine http2Engine = Http2EngineFactory.createHttp2Engine(http2Parser, channel, channel.getRemoteAddress(), SERVER);
        http2Engine.setRequestListener(timedListener);
        http2Engine.sendLocalPreferredSettings();
        maybeSettingsFrame.ifPresent(settingsFrame ->
        {
            Http2Settings settings = (Http2Settings) http2Parser.unmarshal(dataGen.wrapByteBuffer(settingsFrame));
            http2Engine.setRemoteSettings(settings);
        });
        dataListener = http2Engine.getDataListener();
        responseSender = http2Engine.getResponseSender();
    }

    @Override
    public CompletableFuture<Void> closeSocket() {
        // TODO: implement this
        return null;
    }

    @Override
    public Channel getUnderlyingChannel() {
        return channel;
    }

    @Override
    public ResponseSender getResponseSender() {
        return responseSender;
    }

    @Override
    public DataListener getDataListener() {
        return dataListener;
    }
}