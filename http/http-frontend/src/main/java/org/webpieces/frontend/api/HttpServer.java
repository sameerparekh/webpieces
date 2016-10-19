package org.webpieces.frontend.api;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

import org.webpieces.httpcommon.api.HttpSocket;
import org.webpieces.nio.api.channels.TCPServerChannel;

public interface HttpServer {

	CompletableFuture<Void> close();

	void enableOverloadMode(ByteBuffer overloadResponse);

	void disableOverloadMode();

	TCPServerChannel getUnderlyingChannel();

}
