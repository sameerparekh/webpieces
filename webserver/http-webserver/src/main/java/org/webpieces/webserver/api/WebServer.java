package org.webpieces.webserver.api;

import org.webpieces.frontend.api.HttpRequestListener;
import org.webpieces.nio.api.channels.TCPServerChannel;

public interface WebServer {

	HttpRequestListener start();

	void stop();

	TCPServerChannel getUnderlyingHttpChannel();

	TCPServerChannel getUnderlyingHttpsChannel();
	
}
