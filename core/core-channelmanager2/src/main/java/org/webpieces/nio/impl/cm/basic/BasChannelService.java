package org.webpieces.nio.impl.cm.basic;

import javax.net.ssl.SSLEngine;

import org.webpieces.data.api.BufferPool;
import org.webpieces.nio.api.ChannelManager;
import org.webpieces.nio.api.SSLEngineFactory;
import org.webpieces.nio.api.channels.DatagramChannel;
import org.webpieces.nio.api.channels.TCPChannel;
import org.webpieces.nio.api.channels.TCPServerChannel;
import org.webpieces.nio.api.channels.UDPChannel;
import org.webpieces.nio.api.handlers.ConnectionListener;
import org.webpieces.nio.api.handlers.DatagramListener;
import org.webpieces.nio.api.testutil.chanapi.ChannelsFactory;
import org.webpieces.nio.api.testutil.nioapi.SelectorProviderFactory;
import org.webpieces.nio.impl.cm.basic.udp.DatagramChannelImpl;
import org.webpieces.nio.impl.cm.basic.udp.UDPChannelImpl;



/**
 * @author Dean Hiller
 */
class BasChannelService implements ChannelManager {

	private SelectorManager2 selMgr;
    private ChannelsFactory channelFactory;
	private boolean started;
	private BufferPool pool;
	
	BasChannelService(String threadName, ChannelsFactory c, 
			SelectorProviderFactory mgr, BufferPool pool) {
		this.pool = pool;
		selMgr = new SelectorManager2(mgr, pool, threadName);
        this.channelFactory = c;
        start();
	}
	
	@Override
    public TCPServerChannel createTCPServerChannel(String id, ConnectionListener listener) {
        preconditionChecks(id);
        if(listener == null)
        	throw new IllegalArgumentException("connectionListener cannot be null");
        IdObject obj = new IdObject(id);
        return new BasTCPServerChannel(obj, channelFactory, selMgr, listener, pool);
	}
	
	@Override
	public TCPServerChannel createTCPServerChannel(String id, ConnectionListener connectionListener,
			SSLEngineFactory factory) {
		throw new UnsupportedOperationException("SSL not supported at this level.");
	}
	
	private void preconditionChecks(String id) {
		if(id == null)
            throw new IllegalArgumentException("id cannot be null");
		else if(!started)
			throw new IllegalStateException("Call start() on the ChannelManagerService first");
	}
	@Override
    public TCPChannel createTCPChannel(String id) {
        preconditionChecks(id);
        IdObject obj = new IdObject(id);      
        return new BasTCPChannel(obj, channelFactory, selMgr, pool);
	}

	@Override
	public TCPChannel createTCPChannel(String id, SSLEngine engine) {
		throw new UnsupportedOperationException("SSL not supported at this level.");
	}
	
	@Override
    public UDPChannel createUDPChannel(String id) {
        preconditionChecks(id);
        IdObject obj = new IdObject(id);
        return new UDPChannelImpl(obj, selMgr, pool);
    }
    
	@Override
	public DatagramChannel createDatagramChannel(String id, int bufferSize, DatagramListener dataListener) {
        if(dataListener == null)
        	throw new IllegalArgumentException("dataListener cannot be null");
        return new DatagramChannelImpl(id, bufferSize, dataListener);
    }
    
	public void start() {
		started = true;
		selMgr.start();
	}
	
	/* (non-Javadoc)
	 * @see api.biz.xsoftware.nio.ChannelManager#shutdown()
	 */
	public void stop() {
		started = false;
		selMgr.stop();
	}
	
}
