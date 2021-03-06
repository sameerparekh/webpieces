package org.webpieces.asyncserver.impl;

import org.webpieces.asyncserver.api.AsyncConfig;
import org.webpieces.asyncserver.api.AsyncDataListener;
import org.webpieces.asyncserver.api.AsyncServer;
import org.webpieces.asyncserver.api.AsyncServerManager;
import org.webpieces.nio.api.ChannelManager;
import org.webpieces.nio.api.SSLEngineFactory;
import org.webpieces.nio.api.channels.TCPServerChannel;

public class AsyncServerManagerImpl implements AsyncServerManager {

	private ChannelManager channelManager;

	public AsyncServerManagerImpl(ChannelManager channelManager) {
		this.channelManager = channelManager;
	}

	@Override
	public AsyncServer createTcpServer(
			AsyncConfig config, AsyncDataListener listener, SSLEngineFactory sslFactory) {
		if(sslFactory == null)
			throw new IllegalArgumentException("SSLEngineFactory is null but must be supplied");
		return createTcpServerImpl(config, listener, sslFactory);
	}
	
	private AsyncServer createTcpServerImpl(AsyncConfig config,
			AsyncDataListener listener, SSLEngineFactory sslFactory) {
		String id = config.id;
		ConnectedChannels connectedChannels = new ConnectedChannels();
		ProxyDataListener proxyListener = new ProxyDataListener(connectedChannels, listener);
		DefaultConnectionListener connectionListener = new DefaultConnectionListener(connectedChannels, proxyListener); 

		TCPServerChannel serverChannel;
		if(sslFactory != null)
			serverChannel = channelManager.createTCPServerChannel(id, connectionListener, sslFactory);
		else
			serverChannel = channelManager.createTCPServerChannel(id, connectionListener);

		//MUST be called before bind...
		serverChannel.setReuseAddress(true);
		
		serverChannel.configure(config.functionToConfigureBeforeBind);
		serverChannel.bind(config.bindAddr);
		
		return new AsyncServerImpl(serverChannel, connectionListener, proxyListener);
	}

	@Override
	public AsyncServer createTcpServer(AsyncConfig config, AsyncDataListener listener) {
		return createTcpServerImpl(config, listener, null);
	}
}
