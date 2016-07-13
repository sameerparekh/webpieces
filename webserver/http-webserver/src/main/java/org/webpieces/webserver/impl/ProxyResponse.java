package org.webpieces.webserver.impl;

import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webpieces.data.api.DataWrapper;
import org.webpieces.data.api.DataWrapperGenerator;
import org.webpieces.data.api.DataWrapperGeneratorFactory;
import org.webpieces.frontend.api.FrontendSocket;
import org.webpieces.frontend.api.exception.HttpException;
import org.webpieces.httpparser.api.common.Header;
import org.webpieces.httpparser.api.common.KnownHeaderName;
import org.webpieces.httpparser.api.dto.HttpRequest;
import org.webpieces.httpparser.api.dto.HttpResponse;
import org.webpieces.httpparser.api.dto.HttpResponseStatus;
import org.webpieces.httpparser.api.dto.HttpResponseStatusLine;
import org.webpieces.httpparser.api.dto.KnownStatusCode;
import org.webpieces.router.api.ResponseStreamer;
import org.webpieces.router.api.dto.RedirectResponse;
import org.webpieces.router.api.dto.RenderResponse;
import org.webpieces.router.api.dto.View;
import org.webpieces.router.api.exceptions.IllegalReturnValueException;
import org.webpieces.templating.api.Template;
import org.webpieces.templating.api.TemplateService;
import org.webpieces.webserver.api.WebServerConfig;

import groovy.lang.MissingPropertyException;

public class ProxyResponse implements ResponseStreamer {

	private static final Logger log = LoggerFactory.getLogger(ProxyResponse.class);
	private static final DateTimeFormatter formatter = DateTimeFormat.forPattern("E, dd MMM Y HH:mm:ss");
	private static final DataWrapperGenerator wrapperFactory = DataWrapperGeneratorFactory.createDataWrapperGenerator();
	private FrontendSocket channel;
	private HttpRequest request;
	private TemplateService templatingService;
	private WebServerConfig config;

	public ProxyResponse(HttpRequest req, FrontendSocket channel, TemplateService templatingService, WebServerConfig config) {
		this.request = req;
		this.channel = channel;
		this.templatingService = templatingService;
		this.config = config;
	}

	public ProxyResponse(FrontendSocket channel) {
		this.channel = channel;
	}
	
	@Override
	public void sendRedirect(RedirectResponse httpResponse) {
		HttpResponseStatus status = new HttpResponseStatus();
		status.setKnownStatus(KnownStatusCode.HTTP303);
		HttpResponseStatusLine statusLine = new HttpResponseStatusLine();
		statusLine.setStatus(status);
		HttpResponse response = new HttpResponse();
		response.setStatusLine(statusLine);
		
		String url = httpResponse.redirectToPath;
		
		if(httpResponse.domain != null && httpResponse.isHttps != null) {
			String prefix = "http://";
			if(httpResponse.isHttps)
				prefix = "https://";
			url = prefix + httpResponse.domain + httpResponse.redirectToPath;
		} else if(httpResponse.domain != null) {
			throw new IllegalReturnValueException("Controller is returning a domain without returning isHttps=true or"
					+ " isHttps=false so we can form the entire redirect.  Either drop the domain or set isHttps");
		} else if(httpResponse.isHttps != null) {
			throw new IllegalReturnValueException("Controller is returning isHttps="+httpResponse.isHttps+" but there is"
					+ "no domain set so we can't form the full redirect.  Either drop setting isHttps or set the domain");
		}
		
		Header location = new Header(KnownHeaderName.LOCATION, url);
		response.addHeader(location );
		
		//Firefox requires a content length of 0 (chrome doesn't)!!!...
		addCommonHeaders(response, 0);
		
		log.info("sending REDIRECT response channel="+channel);
		channel.write(response);

		closeIfNeeded();
	}

	@Override
	public void sendRenderHtml(RenderResponse resp) {
		HttpResponseStatus status = new HttpResponseStatus();
		if(resp.isNotFoundRoute())
			status.setKnownStatus(KnownStatusCode.HTTP404);
		else
			status.setKnownStatus(KnownStatusCode.HTTP200);
		
		HttpResponseStatusLine statusLine = new HttpResponseStatusLine();
		statusLine.setStatus(status);
		HttpResponse response = new HttpResponse();
		response.setStatusLine(statusLine);

		View view = resp.getView();
		String packageStr = view.getControllerPackage();
		//For this type of View, the template is the name of the method..
		String templateClassName = view.getMethodName();
		
		Template template = templatingService.loadTemplate(packageStr, templateClassName, "html");
		
		//stream this out with chunked response instead....
		StringWriter out = new StringWriter();
		
		Map<String, Object> copy = new HashMap<>(resp.getPageArgs());
		try {
			template.run(copy, out);
		} catch(MissingPropertyException e) {
			Set<String> keys = resp.getPageArgs().keySet();
			throw new ControllerPageArgsException("Controller.method="+view.getControllerName()+"."+view.getMethodName()+" did\nnot"
					+ " return enough arguments for the template.  specifically, the method\nreturned these"
					+ " arguments="+keys+"  The missing properties are as follows....\n"+e.getMessage(), e);
		}
		
		Charset encoding = config.getHtmlResponsePayloadEncoding();
		String content = out.toString();
		byte[] bytes = content.getBytes(encoding);
		
		Header contentType = new Header(KnownHeaderName.CONTENT_TYPE, "text/html; charset="+encoding.name().toLowerCase());
		response.addHeader(contentType);
		
		DataWrapper data = wrapperFactory.wrapByteArray(bytes);
		response.setBody(data);

		addCommonHeaders(response, bytes.length);
		
		log.info("sending RENDERHTML response channel="+channel);
		channel.write(response);
		
		closeIfNeeded();
	}
	
	private void addCommonHeaders(HttpResponse response, int contentLength) {
		
		Header header = new Header(KnownHeaderName.CONTENT_LENGTH, contentLength+"");
		response.addHeader(header);
		
		Header connHeader = request.getHeaderLookupStruct().getHeader(KnownHeaderName.CONNECTION);
		
		DateTime now = DateTime.now().toDateTime(DateTimeZone.UTC);
		String dateStr = formatter.print(now)+" GMT";

		//in general, nearly all these headers are desired..
		Header date = new Header(KnownHeaderName.DATE, dateStr);
		response.addHeader(date);

//		Header xFrame = new Header("X-Frame-Options", "SAMEORIGIN");
//		response.addHeader(xFrame);
		
		//X-XSS-Protection: 1; mode=block
		//X-Frame-Options: SAMEORIGIN
		//Content-Type: image/gif\r\n
	    //Expires: Mon, 20 Jun 2016 02:33:52 GMT\r\n
	    //Cache-Control: private, max-age=31536000\r\n
	    //Last-Modified: Mon, 02 Apr 2012 02:13:37 GMT\r\n
		//X-Content-Type-Options: nosniff\r\n
		
		if(connHeader == null)
			return;
		else if(!"keep-alive".equals(connHeader.getValue()))
			return;

		//just re-use the connHeader from the request...
		response.addHeader(connHeader);
	}

	private void closeIfNeeded() {
		Header connHeader = request.getHeaderLookupStruct().getHeader(KnownHeaderName.CONNECTION);
		boolean close = false;
		if(connHeader != null) {
			String value = connHeader.getValue();
			if(!"keep-alive".equals(value)) {
				close = true;
			}
		} else
			close = true;
		
		if(close)
			channel.close();
	}

	@Override
	public void failure(Throwable e) {
		log.error("Exception", e);
	}

	public void sendFailure(HttpException exc) {
		HttpResponseStatus status = new HttpResponseStatus();
		status.setKnownStatus(exc.getStatusCode());
		
		HttpResponseStatusLine statusLine = new HttpResponseStatusLine();
		statusLine.setStatus(status);
		HttpResponse response = new HttpResponse();
		response.setStatusLine(statusLine);
		
		response.addHeader(new Header("Failure", exc.getMessage()));
		
		channel.write(response);
	}

}