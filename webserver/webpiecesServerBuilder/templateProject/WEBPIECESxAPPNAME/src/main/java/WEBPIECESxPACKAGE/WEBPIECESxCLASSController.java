package WEBPIECESxPACKAGE;

import javax.inject.Inject;

import org.webpieces.router.api.actions.Action;
import org.webpieces.router.api.actions.Actions;

import WEBPIECESxPACKAGE.example.SomeLibrary;

public class WEBPIECESxCLASSController {
	
	@Inject
	private SomeLibrary someLibrary;
	
	public Action myMethod() {
		someLibrary.doSomething();
		//renderThis assumes the view is the <methodName>.html file so in this case
		//myMethod.html which must be in the same directory as the Controller
		return Actions.renderThis(
				"user", "Dean Hiller",
				"id", 500,
				"otherKey", "key");
	}
	
	public Action anotherMethod() {
		return Actions.redirect(WEBPIECESxCLASSRouteId.SOME_ROUTE);
	}
	
}
