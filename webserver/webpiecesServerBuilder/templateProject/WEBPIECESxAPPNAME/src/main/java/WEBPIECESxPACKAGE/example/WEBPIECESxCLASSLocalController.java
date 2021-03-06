package WEBPIECESxPACKAGE.example;

import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

import org.webpieces.router.api.actions.Action;
import org.webpieces.router.api.actions.Actions;

import WEBPIECESxPACKAGE.WEBPIECESxCLASSRouteId;

public class WEBPIECESxCLASSLocalController {

	@Inject
	private RemoteService service;

	public Action index() {
		return Actions.renderThis();
	}
	
	public Action exampleList() {
		return Actions.renderThis("user", "Dean Hiller");
	}
	
	public CompletableFuture<Action> myAsyncMethod() {
		CompletableFuture<Integer> remoteValue = service.fetchRemoteValue();
		return remoteValue.thenApply(s -> convertToAction(s));
	}
	
	private Action convertToAction(int value) {
		return Actions.renderThis("value", value);
	}
	
	public Action redirect(String id) {
		return Actions.redirect(WEBPIECESxCLASSRouteId.RENDER_PAGE);
	}
	
	public Action notFound() {
		return Actions.renderThis();
	}
	
	public Action internalError() {
		return Actions.renderThis();
	}
}
