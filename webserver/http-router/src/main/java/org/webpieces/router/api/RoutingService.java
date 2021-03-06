package org.webpieces.router.api;

import java.util.Map;

import org.webpieces.ctx.api.RouterRequest;

public interface RoutingService {

	void start();

	void stop();

	void processHttpRequests(RouterRequest req, ResponseStreamer streamer);

	/**
	 * This is exposed as the webserver wires router and templating engine and the templating engine needs a callback to
	 * reverse all routeIds in the html file to actual urls which only the router has knowledge of.
	 * 
	 * @param routeId
	 * @param args
	 * @return
	 */
	String convertToUrl(String routeId, Map<String, String> notUrlEncodedArgs);
	
}
