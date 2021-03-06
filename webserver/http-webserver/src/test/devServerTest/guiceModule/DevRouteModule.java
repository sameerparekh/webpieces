package org.webpieces.webserver.dev.app;

import static org.webpieces.ctx.api.HttpMethod.GET;

import org.webpieces.router.api.routing.AbstractRouteModule;

public class DevRouteModule extends AbstractRouteModule {

	@Override
	public void configure(String currentPackage) {
		addRoute(GET , "/home",               "DevController.home", DevRouteId.HOME);
		
		addRoute(GET , "/causeError",         "DevController.causeError", DevRouteId.CAUSE_ERROR);

		setPageNotFoundRoute("DevController.notFound");
		setInternalErrorRoute("DevController.internalError");
	}

}
