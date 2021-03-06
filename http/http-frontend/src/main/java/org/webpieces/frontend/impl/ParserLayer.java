package org.webpieces.frontend.impl;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.webpieces.data.api.DataWrapper;
import org.webpieces.data.api.DataWrapperGenerator;
import org.webpieces.data.api.DataWrapperGeneratorFactory;
import org.webpieces.frontend.api.FrontendConfig;
import org.webpieces.frontend.api.FrontendSocket;
import org.webpieces.frontend.api.exception.HttpClientException;
import org.webpieces.frontend.api.exception.HttpException;
import org.webpieces.httpparser.api.HttpParser;
import org.webpieces.httpparser.api.Memento;
import org.webpieces.httpparser.api.ParseException;
import org.webpieces.httpparser.api.UnparsedState;
import org.webpieces.httpparser.api.dto.HttpMessageType;
import org.webpieces.httpparser.api.dto.HttpPayload;
import org.webpieces.httpparser.api.dto.HttpRequest;
import org.webpieces.httpparser.api.dto.KnownStatusCode;
import org.webpieces.nio.api.channels.Channel;
import org.webpieces.nio.api.channels.ChannelSession;

public class ParserLayer {

	private static final DataWrapperGenerator generator = DataWrapperGeneratorFactory.createDataWrapperGenerator();
	private HttpParser parser;
	private TimedListener listener;
	private boolean isHttps;
	private FrontendConfig config;
	
	public ParserLayer(HttpParser parser2, TimedListener listener, FrontendConfig config, boolean isHttps) {
		this.parser = parser2;
		this.listener = listener;
		this.config = config;
		this.isHttps = isHttps;
	}

	public void deserialize(Channel channel, ByteBuffer chunk) {
		List<HttpRequest> parsedRequests = doTheWork(channel, chunk);
		
		for(HttpRequest req : parsedRequests) {
			listener.processHttpRequests(translate(channel), req, isHttps);
		}
	}

	private List<HttpRequest> doTheWork(Channel channel, ByteBuffer chunk) {
		ChannelSession session = channel.getSession();		
		Memento memento = (Memento) session.get("memento");
		
		if(memento == null) {
			memento = parser.prepareToParse();
			session.put("memento", memento);
		}

		DataWrapper dataWrapper = generator.wrapByteBuffer(chunk);
		
		Memento resultMemento = parse(memento, dataWrapper);

		List<HttpPayload> parsedMsgs = resultMemento.getParsedMessages();
		List<HttpRequest> parsedRequests = new ArrayList<>();
		for(HttpPayload msg : parsedMsgs) {
			if(msg.getMessageType() != HttpMessageType.REQUEST)
				throw new ParseException("Wrong message type="+msg.getMessageType()+" should be="+HttpMessageType.REQUEST);
			parsedRequests.add(msg.getHttpRequest());
		}
		return parsedRequests;
	}

	private Memento parse(Memento memento, DataWrapper dataWrapper) {
		Memento resultMemento = parser.parse(memento, dataWrapper);
		
		UnparsedState unParsedState = resultMemento.getUnParsedState();
		switch (unParsedState.getCurrentlyParsing()) {
		case HEADERS:
			if(unParsedState.getCurrentUnparsedSize() > config.maxHeaderSize)
				throw new HttpClientException("Max heaader size="+config.maxHeaderSize+" was exceeded", KnownStatusCode.HTTP_431_REQUEST_HEADERS_TOO_LARGE);
			break;
		case BODY:
		case CHUNK:
			if(unParsedState.getCurrentUnparsedSize() > config.maxBodyOrChunkSize)
				throw new HttpClientException("Body or chunk size limit exceeded", KnownStatusCode.HTTP_413_PAYLOAD_TOO_LARGE);
		default:
			break;
		}
		
		return resultMemento;
	}

	public void sendServerResponse(Channel channel, HttpException exc) {
		FrontendSocket socket = translate(channel);
		listener.sendServerResponse(socket, exc);
	}

	public void openedConnection(Channel channel, boolean isReadyForWrites) {
		FrontendSocket socket = translate(channel);
		listener.clientOpenChannel(socket, isReadyForWrites);
	}
	
	public void farEndClosed(Channel channel) {
		FrontendSocket socket = translate(channel);
		listener.clientClosedChannel(socket);		
	}

	public void applyWriteBackPressure(Channel channel) {
		FrontendSocket socket = translate(channel);
		listener.applyWriteBackPressure(socket);
	}

	public void releaseBackPressure(Channel channel) {
		FrontendSocket socket = translate(channel);
		listener.releaseBackPressure(socket);
	}

	private FrontendSocket translate(Channel channel) {
		ChannelSession session = channel.getSession();
		FrontendSocketImpl socket = (FrontendSocketImpl) session.get("webpieces.frontendSocket");
		if(socket == null) {
			socket = new FrontendSocketImpl(channel, parser);
			session.put("webpieces.frontendSocket", socket);
		}
		return socket;
	}

}
