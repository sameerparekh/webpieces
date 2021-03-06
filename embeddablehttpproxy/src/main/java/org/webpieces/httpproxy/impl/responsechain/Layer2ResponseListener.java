package org.webpieces.httpproxy.impl.responsechain;

import java.nio.channels.UnresolvedAddressException;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webpieces.frontend.api.FrontendSocket;
import org.webpieces.frontend.api.exception.HttpClientException;
import org.webpieces.frontend.api.exception.HttpServerException;
import org.webpieces.httpclient.api.HttpSocket;
import org.webpieces.httpparser.api.dto.HttpPayload;
import org.webpieces.httpparser.api.dto.HttpRequest;
import org.webpieces.httpparser.api.dto.KnownStatusCode;
import org.webpieces.httpproxy.impl.chain.LayerZSendBadResponse;
import org.webpieces.nio.api.channels.Channel;

public class Layer2ResponseListener {

	private static final Logger log = LoggerFactory.getLogger(Layer2ResponseListener.class);

	@Inject
	private LayerZSendBadResponse badResponse;
	
	public void processResponse(FrontendSocket channel, HttpRequest req, HttpPayload resp, boolean isComplete) {
		log.info("received response(channel="+channel+").  type="+resp.getClass().getSimpleName()+" complete="+isComplete+" resp=\n"+resp);

		channel.write(resp)
			.thenAccept(p -> wroteBytes(channel))
			.exceptionally(e -> failedWrite(channel, e));
	}

	private Void failedWrite(FrontendSocket channel, Throwable e) {
		log.error("failed to respond to channel="+channel, e);
		return null;
	}

	private void wroteBytes(FrontendSocket channel) {
		log.info("wrote bytes out channel="+channel);
	}

	public Void processError(FrontendSocket channel, HttpRequest req, Throwable e) {
		log.error("could not process req="+req+" from channel="+channel+" due to exception", e);

		if(e.getCause() instanceof UnresolvedAddressException) {
			HttpClientException exc = new HttpClientException("Client gave a bad address to connect to", KnownStatusCode.HTTP_404_NOTFOUND, e);
			badResponse.sendServerResponse(channel, exc);
		} else {
			HttpServerException exc = new HttpServerException("Server has a bug", KnownStatusCode.HTTP_500_INTERNAL_SVR_ERROR, e);
			badResponse.sendServerResponse(channel, exc);
		}
		
		channel.close();
		
		return null;
	}

	public void farEndClosed(HttpSocket socket, Channel channel) {
		//since socket is closing, close the channel from the browser...
		log.info("closing connection from browser.  channel="+channel);
		channel.close();
	}

}
