package org.webpieces.router.api.dto;

import java.util.Map;

import org.webpieces.router.api.actions.Render;

public class RenderResponse implements Render {

	public View view;
	public RouteType routeType;
	public Map<String, Object> pageArgs;

	public RenderResponse(View view2, Map<String, Object> pageArgs, RouteType routeType) {
		this.view = view2;
		this.pageArgs = pageArgs;
		this.routeType = routeType;
	}

}
