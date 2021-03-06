package org.webpieces.webserver.i18n;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.webpieces.frontend.api.HttpRequestListener;
import org.webpieces.httpparser.api.common.Header;
import org.webpieces.httpparser.api.common.KnownHeaderName;
import org.webpieces.httpparser.api.dto.HttpRequest;
import org.webpieces.httpparser.api.dto.KnownHttpMethod;
import org.webpieces.httpparser.api.dto.KnownStatusCode;
import org.webpieces.templating.api.TemplateCompileConfig;
import org.webpieces.util.file.VirtualFileClasspath;
import org.webpieces.webserver.Requests;
import org.webpieces.webserver.WebserverForTest;
import org.webpieces.webserver.test.FullResponse;
import org.webpieces.webserver.test.MockFrontendSocket;
import org.webpieces.webserver.test.PlatformOverridesForTest;

public class TestI18n {

	private HttpRequestListener server;
	private MockFrontendSocket socket = new MockFrontendSocket();

	@Before
	public void setUp() {
		TemplateCompileConfig config = new TemplateCompileConfig();
		VirtualFileClasspath metaFile = new VirtualFileClasspath("i18nMeta.txt", WebserverForTest.class.getClassLoader());
		WebserverForTest webserver = new WebserverForTest(new PlatformOverridesForTest(config), null, false, metaFile);
		server = webserver.start();
	}

	@Test
	public void testDefaultText() {
		HttpRequest req = Requests.createRequest(KnownHttpMethod.GET, "/i18nBasic");
		
		server.processHttpRequests(socket, req , false);
		
		List<FullResponse> responses = socket.getResponses();
		Assert.assertEquals(1, responses.size());

		FullResponse response = responses.get(0);
		response.assertStatusCode(KnownStatusCode.HTTP_200_OK);
		response.assertContains("This is the default text that would go here and can be quite a long\n      version");
		response.assertContains("Hi Dean, we would like to take you to Italy");
	}

	@Test
	public void testChineseText() {
		HttpRequest req = Requests.createRequest(KnownHttpMethod.GET, "/i18nBasic");
		req.addHeader(new Header(KnownHeaderName.ACCEPT_LANGUAGE, "zh-CN"));
		
		server.processHttpRequests(socket, req , false);
		
		List<FullResponse> responses = socket.getResponses();
		Assert.assertEquals(1, responses.size());

		FullResponse response = responses.get(0);
		response.assertStatusCode(KnownStatusCode.HTTP_200_OK);
		response.assertContains("你好， 这个是一个比较长的一个东西 我可以写比较多。  我在北京师范大学学了中文。 我喜欢完冰球");
		response.assertContains("你好Dean，我们要去Italy");
	}
}
