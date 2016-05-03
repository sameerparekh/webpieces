package com.webpieces.httpparser.api.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.webpieces.httpparser.api.common.Header;
import com.webpieces.httpparser.api.common.KnownHeaderName;

public abstract class HttpMsg2 extends HttpMessage {

	protected List<Header> headers = new ArrayList<>();
	//Convenience structure that further morphs the headers into a Map that can
	//be looked up by key.
	private transient Headers headersStruct = new Headers();
	
	/**
	 * Order of HTTP Headers matters for Headers with the same key
	 * 
	 * @param headers
	 */
	public List<Header> getHeaders() {
		return Collections.unmodifiableList(headers);
	}

	public void addHeader(Header header) {
		headers.add(header);
		headersStruct.addHeader(header);
	}
	
	/** 
	 * 
	 * @return
	 */
	public Headers getHeaderLookupStruct() {
		return headersStruct;
	}
	
	public boolean isHasChunkedTransferHeader() {
		//need to account for a few Transfer Encoding headers
		Header header = headersStruct.getLastInstanceOfHeader(KnownHeaderName.TRANSFER_ENCODING);
		if("chunked".equals(header.getValue()))
			return true;
		return false;
	}
}
