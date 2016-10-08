package org.webpieces.plugins.hibernate.app;

import static org.webpieces.ctx.api.HttpMethod.*;

import org.webpieces.router.api.routing.AbstractRouteModule;

public class HibernateRouteModule extends AbstractRouteModule {

	@Override
	protected void configure(String currentPackage) {
		addRoute(POST , "/save",      "HibernateController.save", HibernateRouteId.SAVE_ENTITY);

		setPageNotFoundRoute("/org/webpieces/webserver/basic/app/biz/BasicController.notFound");
		setInternalErrorRoute("/org/webpieces/webserver/basic/app/biz/BasicController.internalError");
	}

}
