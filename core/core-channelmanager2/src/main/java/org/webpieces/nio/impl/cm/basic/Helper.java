package org.webpieces.nio.impl.cm.basic;

import java.io.IOException;
import java.net.PortUnreachableException;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SelectionKey;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webpieces.data.api.BufferPool;
import org.webpieces.nio.api.channels.Channel;
import org.webpieces.nio.api.handlers.DataListener;
import org.webpieces.nio.api.testutil.nioapi.Select;


public final class Helper {

	private static final Logger apiLog = LoggerFactory.getLogger(DataListener.class);
	private static final Logger log = LoggerFactory.getLogger(Helper.class);
	//private static BufferHelper helper = ChannelManagerFactory.bufferHelper(null);
	private static boolean logBufferNextRead = false;

	private Helper() {}
	
	public static String opType(int ops) {
		String retVal = "";
		if((ops & SelectionKey.OP_ACCEPT) > 0)
			retVal+="A";
		if((ops & SelectionKey.OP_CONNECT) > 0)
			retVal+="C";
		if((ops & SelectionKey.OP_READ) > 0)
			retVal+="R";
		if((ops & SelectionKey.OP_WRITE) > 0)
			retVal+="W";
		
		return retVal;
	}
	
	public static void processKeys(Set<SelectionKey> keySet, SelectorManager2 mgr, BufferPool pool) {
		Iterator<SelectionKey> iter = keySet.iterator();
		while (iter.hasNext()) {
			SelectionKey key = null;
			try {
				key = iter.next();
				if(log.isTraceEnabled())
					log.trace(key.attachment()+" ops="+Helper.opType(key.readyOps())
							+" acc="+key.isAcceptable()+" read="+key.isReadable()+" write"+key.isWritable());
				processKey(key, mgr, pool);
//			} catch(CancelledKeyException e) {
//				log.log(Level.INFO, "Cancelled key may be normal", e);
			} catch(IOException e) {
				log.error(key.attachment()+"Processing of key failed, closing channel", e);
				try {
					if(key != null) 
						key.channel().close();
				} catch(Throwable ee) {
					log.error(key.attachment()+"Close of channel failed", ee);
				}
			} catch(CancelledKeyException e) {
				//TODO: get rid of this if...else statement by fixing
				//CancelledKeyException on linux so the tests don't fail				
				log.trace(key.attachment()+"Processing of key failed, but continuing channel manager loop", e);				
			} catch(Throwable e) {
				log.error(key.attachment()+"Processing of key failed, but continuing channel manager loop", e);
				try {
					key.cancel();
				} catch(Throwable ee) {
					log.info("cancelling key failed.  exception type="+ee.getClass()+" msg="+ee.getMessage());
				}
			}
		}
		//clear the whole keySet as we processed them all in the while loop.

		//If you do not clear the keySet, keys that have been already processed stay
		//in the selected Key set.  If another key gets added to this set, the selector
		//goes off again and has the stale key plus the new key and the stale key
		//is processed again.
		keySet.clear();
	}
	
	private static void processKey(SelectionKey key, SelectorManager2 mgr, BufferPool pool) throws IOException, InterruptedException {
		if(log.isTraceEnabled())
			log.trace(key.attachment()+"proccessing");

		//This is code to try to avoid the CancelledKeyExceptions
		if(!key.channel().isOpen() || !key.isValid())
			return;
		
		//if isAcceptable, than is a ServerSocketChannel
		if(key.isAcceptable()) {
			Helper.acceptSocket(key);
		} 
		
		if(key.isConnectable())
			Helper.connect(key);
		
		if(key.isWritable()) {
			Helper.write(key);
		}
            
		//The read MUST be after the write as a call to key.isWriteable is invalid if the
		//read resulted in the far end closing the socket.
		if(key.isReadable()) {
			Helper.read(key, mgr, pool);
		}                   
	}
	
	//each of these functions should be a handler for a new type that we set up
	//on the outside of this thing.  The signature is the same thing every time
	// and we pass key and the Channel.  We can cast to the proper one.
	private static void acceptSocket(SelectionKey key) throws IOException {
//		SelectableChannel s = key.channel();		
		if(log.isTraceEnabled())
			log.trace(key.attachment()+"Incoming Connection="+key);
		
		WrapperAndListener struct = (WrapperAndListener)key.attachment();
		BasTCPServerChannel channel = (BasTCPServerChannel)struct.getChannel();
		channel.accept(channel.getChannelCount());
	}
	
	private static void connect(SelectionKey key) throws IOException {
		if(log.isTraceEnabled())
			log.trace(key.attachment()+"finishing connect process");
		
		WrapperAndListener struct = (WrapperAndListener)key.attachment();
		CompletableFuture<Channel> callback = struct.getConnectCallback();
		BasTCPChannel channel = (BasTCPChannel)struct.getChannel();
		
		//must change the interests to not interested in connect anymore
		//since we are connected otherwise selector seems to keep firing over
		//and over again with 0 keys wasting cpu like a while(true) loop
		int interests = key.interestOps();
		key.interestOps(interests & (~SelectionKey.OP_CONNECT));
		
		try {
			channel.finishConnect();
			callback.complete(channel);
		} catch(Exception e) {
            log.error(key.attachment()+"Could not open connection", e);
            callback.completeExceptionally(e);
		}
	}

	private static void read(SelectionKey key, SelectorManager2 mgr, BufferPool pool) throws IOException {
		if(log.isTraceEnabled())
			log.trace(key.attachment()+"reading data");
		
		WrapperAndListener struct = (WrapperAndListener)key.attachment();
		DataListener in = struct.getDataHandler();
		BasChannelImpl channel = (BasChannelImpl)struct.getChannel();
		
		//if someone JUST unregistered for reads, then let's not read from this socket since it would put more
		//pressure in RAM so just wait until they re-registerForReads and they will get the data then
		if(!channel.isRegisteredForReads()) {
			//log.info("not registered for reads so skipping");
			return; //do not process reads if we were unregistered
		}
		
		ByteBuffer chunk = pool.nextBuffer(512);
		
		try {
            if(logBufferNextRead)
            	log.info(channel+"buffer="+chunk);
            int bytes = channel.readImpl(chunk);
            if(logBufferNextRead) {
            	logBufferNextRead = false;
            	log.info(channel+"buffer2="+chunk);                	
            }

            processBytes(key, chunk, bytes, mgr);
            
		} catch(PortUnreachableException e) {
            //this is a normal occurence when some writes out udp to a port that is not
            //listening for udp!!!  log as finer and fire to client to deal with it.
			log.trace("Client sent data to a host or port that is not listening " +
                    "to udp, or udp can't get through to that machine", e);
			in.failure(channel, null, e);
        } catch(NotYetConnectedException e) {
            //this happens in udp when I disconnect after someone had already been streaming me
            //data.  It is supposed to stop listening but selector keeps firing.
            log.error("Can't read until UDPChannel is connected", e);
            in.failure(channel, null, e);
		} catch(IOException e) {
            //kept getting the following exception so I added this as protection
            //NOTE: this exceptionn should never occur.  read should be returning -1
            //but for some reason is returning hte exception instead.
//        WARNING: [server] Processing of key failed, closing channel
//        java.io.IOException: An established connection was aborted by the software in your host machine
//            at sun.nio.ch.SocketDispatcher.read0(Native Method)
//            at sun.nio.ch.SocketDispatcher.read(SocketDispatcher.java:25)
//            at sun.nio.ch.IOUtil.readIntoNativeBuffer(IOUtil.java:233)
//            at sun.nio.ch.IOUtil.read(IOUtil.java:206)
//            at sun.nio.ch.SocketChannelImpl.read(SocketChannelImpl.java:207)
//            at biz.xsoftware.impl.nio.cm.basic.TCPChannelImpl.readImpl(TCPChannelImpl.java:168)
//            at biz.xsoftware.impl.nio.cm.basic.Helper.read(Helper.java:128)
//            at biz.xsoftware.impl.nio.cm.basic.Helper.processKey(Helper.java:77)
//            at biz.xsoftware.impl.nio.cm.basic.Helper.processKeys(Helper.java:43)
//            at biz.xsoftware.impl.nio.cm.basic.SelectorManager2.runLoop(SelectorManager2.java:262)
//            at biz.xsoftware.impl.nio.cm.basic.SelectorManager2$PollingThread.run
//        (SelectorManager2.java:224)     
            
			//another one that landes here is the Connection reset by peer"....
//			Jan 18, 2012 1:00:42 PM biz.xsoftware.impl.nio.cm.basic.Helper read
//			INFO: [stserver] Exception
//			java.io.IOException: Connection reset by peer
//			        at sun.nio.ch.FileDispatcher.read0(Native Method)
//			        at sun.nio.ch.SocketDispatcher.read(SocketDispatcher.java:39)
//			        at sun.nio.ch.IOUtil.readIntoNativeBuffer(IOUtil.java:251)
//			        at sun.nio.ch.IOUtil.read(IOUtil.java:224)
//			        at sun.nio.ch.SocketChannelImpl.read(SocketChannelImpl.java:254)
//			        at biz.xsoftware.impl.nio.cm.basic.chanimpl.SocketChannelImpl.read(SocketChannelImpl.java:65)
//			        at biz.xsoftware.impl.nio.cm.basic.BasTCPChannel.readImpl(BasTCPChannel.java:108)
//			        at biz.xsoftware.impl.nio.cm.basic.Helper.read(Helper.java:162)
//			        at biz.xsoftware.impl.nio.cm.basic.Helper.processKey(Helper.java:104)
//			        at biz.xsoftware.impl.nio.cm.basic.Helper.processKeys(Helper.java:51)
//			        at biz.xsoftware.impl.nio.cm.basic.SelectorManager2.selectorFired(SelectorManager2.java:253)
//			        at biz.xsoftware.impl.nio.cm.basic.nioimpl.SelectorImpl.runLoop(SelectorImpl.java:126)
//			        at biz.xsoftware.impl.nio.cm.basic.nioimpl.SelectorImpl$PollingThread.run(SelectorImpl.java:107)
			
			//One other exception starts with "An existing connection was forcibly closed"
			
            //in the case of SSL over TCP only, sometimes instead of reading off a -1, this will
            //throw an IOException: "An existing connection was forcibly closed by the remote host"
            //we also close UDPChannels as well on IOException.  Not sure if this is good or not.
			
			String msg = e.getMessage();
			if(msg != null && 
				(msg.startsWith("An existing connection was forcibly closed")
					|| msg.startsWith("Connection reset by peer")
					|| msg.startsWith("An established connection was aborted by the software in your host machine"))) {
		            log.trace("Exception 2", e);
		            processBytes(key, chunk, -1, mgr);
			} else {
				log.error("IO Exception unexpected", e);
				in.failure(channel, null, e);
			}
		}
	}

    /**
     * @param id
     * @param b
     * @param bytes
     * @param mgr 
     * @throws IOException
     */
    private static void processBytes(SelectionKey key, ByteBuffer data, int bytes, SelectorManager2 mgr) throws IOException
    {
        WrapperAndListener struct = (WrapperAndListener)key.attachment();
        DataListener in = struct.getDataHandler();
        BasChannelImpl channel = (BasChannelImpl)struct.getChannel();
        
        ByteBuffer b = data;
        b.flip();
        
		if(bytes < 0) {
			if(apiLog.isTraceEnabled())
				apiLog.trace(channel+"far end closed, cancel key, close socket");
			channel.closeOnSelectorThread();
			in.farEndClosed(channel);
		} else if(bytes > 0) {
			if(apiLog.isTraceEnabled())
				apiLog.trace(channel+"READ bytes="+bytes);
			in.incomingData(channel, b);
		}
    }
    
	private static void write(SelectionKey key) throws IOException, InterruptedException {
		if(log.isTraceEnabled())
			log.trace(key.attachment()+"writing data");
		
		WrapperAndListener struct = (WrapperAndListener)key.attachment();
		BasChannelImpl channel = (BasChannelImpl)struct.getChannel();		
		
		if(log.isTraceEnabled())
			log.trace(channel+"notifying channel of write");

        channel.writeAll();  
	}
	
	static void unregisterSelectableChannel(RegisterableChannelImpl channel, int ops) {
		SelectorManager2 mgr = channel.getSelectorManager();		
		if(!Thread.currentThread().equals(mgr.getThread()))
			throw new RuntimeException(channel+"Bug, changing selector keys can only be done " +
					"on registration thread because there is not synchronization");

		//this could be dangerous and result in deadlock....may want
		//to move this to the selector thread from jdk bugs!!!
		//but alas, follow KISS, move on...
        Select select = channel.getSelectorManager().getSelector();
        SelectionKey key = channel.keyFor(select);
        if(key == null || !key.isValid()) //no need to unregister, key is cancelled
            return;

		int previous = key.interestOps();
		int opsNow = previous & ~ops; //subtract out the operation
		
		key.interestOps(opsNow);
		
		//log.info("unregistering="+Helper.opType(opsNow)+" opToSubtract="+Helper.opType(ops)+" previous="+Helper.opType(previous)+" type="+type);
		
		//make sure we remove the appropriate listener and clean up
		if(key.attachment() != null) {
			WrapperAndListener struct = (WrapperAndListener)key.attachment();
			struct.removeListener(ops);
		}
	}
}
