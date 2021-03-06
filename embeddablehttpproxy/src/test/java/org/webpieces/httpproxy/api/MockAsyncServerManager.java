package org.webpieces.httpproxy.api;

import java.util.ArrayList;
import java.util.List;

import org.webpieces.asyncserver.api.AsyncConfig;
import org.webpieces.asyncserver.api.AsyncDataListener;
import org.webpieces.asyncserver.api.AsyncServer;
import org.webpieces.asyncserver.api.AsyncServerManager;
import org.webpieces.nio.api.SSLEngineFactory;

public class MockAsyncServerManager implements AsyncServerManager {

	private List<AsyncDataListener> serverListeners = new ArrayList<>();


	public List<AsyncDataListener> getServerListeners() {
		return serverListeners;
	}

	@Override
	public AsyncServer createTcpServer(AsyncConfig config, AsyncDataListener listener) {
		serverListeners.add(listener);
		return null;
	}

	@Override
	public AsyncServer createTcpServer(AsyncConfig config, AsyncDataListener listener,
			SSLEngineFactory sslFactory) {
		serverListeners.add(listener);
		return null;
	}

}
