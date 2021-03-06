package org.webpieces.httpparser.impl;

import java.util.ArrayList;
import java.util.List;

import org.webpieces.data.api.DataWrapper;
import org.webpieces.httpparser.api.Memento;
import org.webpieces.httpparser.api.ParsedStatus;
import org.webpieces.httpparser.api.ParsingState;
import org.webpieces.httpparser.api.UnparsedState;
import org.webpieces.httpparser.api.dto.HttpPayload;

public class MementoImpl implements Memento {

	//State held to keep parsing messages
	private List<Integer> leftOverMarkedPositions = new ArrayList<>();
	private DataWrapper leftOverData;
	private int numBytesLeftToRead;
	//The parsed message that did not get the data for it's body just yet
	//This is only for the case where a message has a body
	private HttpPayload halfParsedMessage;
	//If the stream is expecting chunks of data
	private boolean inChunkParsingMode;
	
	//Return state for client to access
	private ParsedStatus status = ParsedStatus.NEED_MORE_DATA;
	private List<HttpPayload> parsedMessages = new ArrayList<>();
	private int indexBytePointer;

	public void setStatus(ParsedStatus status) {
		this.status = status;
	}

	@Override
	public ParsedStatus getStatus() {
		return status;
	}

	@Override
	public List<HttpPayload> getParsedMessages() {
		return parsedMessages;
	}

	public void setParsedMessages(List<HttpPayload> parsedMessages) {
		this.parsedMessages = parsedMessages;
	}

	public DataWrapper getLeftOverData() {
		return leftOverData;
	}

	public void setLeftOverData(DataWrapper data) {
		this.leftOverData = data;
	}

	public void addDemarcation(int i) {
		leftOverMarkedPositions.add(i);
	}

	public List<Integer> getLeftOverMarkedPositions() {
		return leftOverMarkedPositions;
	}

	public void setLeftOverMarkedPositions(List<Integer> leftOverMarkedPositions) {
		this.leftOverMarkedPositions = leftOverMarkedPositions;
	}

	public int getNumBytesLeftToRead() {
		return numBytesLeftToRead;
	}

	public void setNumBytesLeftToRead(int length) {
		numBytesLeftToRead = length;
	}

	public void setHalfParsedMessage(HttpPayload message) {
		this.halfParsedMessage = message;
	}

	public HttpPayload getHalfParsedMessage() {
		return halfParsedMessage;
	}

	public void setReadingHttpMessagePointer(int indexBytePointer) {
		this.indexBytePointer = indexBytePointer;
	}

	public int getReadingHttpMessagePointer() {
		return indexBytePointer;
	}

	public void setInChunkParsingMode(boolean inChunkParsingMode) {
		this.inChunkParsingMode = inChunkParsingMode;
	}

	public boolean isInChunkParsingMode() {
		return inChunkParsingMode;
	}

	@Override
	public UnparsedState getUnParsedState() {
		if(inChunkParsingMode) {
			return new UnparsedState(ParsingState.CHUNK, leftOverData.getReadableSize());
		} else if(halfParsedMessage != null) {
			return new UnparsedState(ParsingState.BODY, leftOverData.getReadableSize());
		}
		
		return new UnparsedState(ParsingState.HEADERS, leftOverData.getReadableSize());
	}
	
}
