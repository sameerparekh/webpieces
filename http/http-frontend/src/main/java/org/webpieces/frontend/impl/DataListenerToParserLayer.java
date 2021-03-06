package org.webpieces.frontend.impl;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webpieces.asyncserver.api.AsyncDataListener;
import org.webpieces.frontend.api.exception.HttpClientException;
import org.webpieces.frontend.api.exception.HttpException;
import org.webpieces.frontend.api.exception.HttpServerException;
import org.webpieces.httpparser.api.ParseException;
import org.webpieces.httpparser.api.dto.KnownStatusCode;
import org.webpieces.nio.api.channels.Channel;
import org.webpieces.nio.api.channels.TCPChannel;

public class DataListenerToParserLayer implements AsyncDataListener {

	private static final Logger log = LoggerFactory.getLogger(DataListenerToParserLayer.class);
	
	private ParserLayer processor;
	
	public DataListenerToParserLayer(ParserLayer nextStage) {
		this.processor = nextStage;
	}

	@Override
	public void connectionOpened(TCPChannel channel, boolean isReadyForWrites) {
		processor.openedConnection(channel, isReadyForWrites);
	}
	
	public void incomingData(Channel channel, ByteBuffer b){
		try {
			InetSocketAddress addr = channel.getRemoteAddress();
			channel.setName(""+addr);
			if(log.isTraceEnabled())
				log.trace("incoming data. size="+b.remaining()+" channel="+channel);
			processor.deserialize(channel, b);
		} catch(ParseException e) {
			HttpClientException exc = new HttpClientException("Could not parse http request", KnownStatusCode.HTTP_400_BADREQUEST, e);
			//move down to debug level later on..
			log.info("Client screwed up", exc);
			sendBadResponse(channel, exc);
		} catch(Throwable e) {
			HttpServerException exc = new HttpServerException("There was a bug in the server, please see the server logs", KnownStatusCode.HTTP_500_INTERNAL_SVR_ERROR, e);
			log.error("Exeption processing", exc);
			sendBadResponse(channel, exc);
		}
	}

	private void sendBadResponse(Channel channel, HttpException exc) {
		try {
			processor.sendServerResponse(channel, exc);
		} catch(Throwable e) {
			log.info("Could not send response to client", e);
		}
	}

	public void farEndClosed(Channel channel) {
		if(log.isTraceEnabled())
			log.trace("far end closed. channel="+channel);
		processor.farEndClosed(channel);
	}

	public void failure(Channel channel, ByteBuffer data, Exception e) {
		log.info("Failure on channel="+channel, e);
		channel.close();
	}

	@Override
	public void applyBackPressure(Channel channel) {
		log.error("Need to apply backpressure", new RuntimeException("demonstrates how we got here"));
		processor.applyWriteBackPressure(channel);
	}

	@Override
	public void releaseBackPressure(Channel channel) {
		log.info("can release backpressure");
		processor.releaseBackPressure(channel);
	}

}
