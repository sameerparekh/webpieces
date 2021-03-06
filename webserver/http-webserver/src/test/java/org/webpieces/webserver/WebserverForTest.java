package org.webpieces.webserver;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.channels.ServerSocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webpieces.frontend.api.HttpRequestListener;
import org.webpieces.nio.api.channels.TCPServerChannel;
import org.webpieces.router.api.RouterConfig;
import org.webpieces.templating.api.TemplateConfig;
import org.webpieces.util.file.VirtualFile;
import org.webpieces.util.file.VirtualFileClasspath;
import org.webpieces.util.security.SecretKeyInfo;
import org.webpieces.webserver.api.WebServer;
import org.webpieces.webserver.api.WebServerConfig;
import org.webpieces.webserver.api.WebServerFactory;

import com.google.inject.Module;

public class WebserverForTest {
	
	private static final Logger log = LoggerFactory.getLogger(WebserverForTest.class);
	public static final Charset CHAR_SET_TO_USE = StandardCharsets.UTF_8;
	
	public static void main(String[] args) throws InterruptedException {
		new WebserverForTest(null, null, false, null).start();
		
		synchronized (WebserverForTest.class) {
			//wait forever for now so server doesn't shut down..
			WebserverForTest.class.wait();
		}	
	}

	private WebServer webServer;

	public WebserverForTest(Module platformOverrides, Module appOverrides, boolean usePortZero, VirtualFile metaFile) {
		String filePath = System.getProperty("user.dir");
		log.info("property user.dir="+filePath);
		
		//Tests can override this...
		if(metaFile == null)
			metaFile = new VirtualFileClasspath("basicMeta.txt", WebserverForTest.class.getClassLoader());
		
		int httpPort = 8080;
		int httpsPort = 8443;
		if(usePortZero) {
			httpPort = 0;
			httpsPort = 0;
		}
		
		
		File cacheDir =  new File(System.getProperty("java.io.tmpdir")+"/webpiecesTestCache");
		//3 pieces to the webserver so a configuration for each piece
		WebServerConfig config = new WebServerConfig()
				.setPlatformOverrides(platformOverrides)
				.setHttpListenAddress(new InetSocketAddress(httpPort))
				.setHttpsListenAddress(new InetSocketAddress(httpsPort))
				.setSslEngineFactory(new SSLEngineFactoryWebServerTesting())
				.setFunctionToConfigureServerSocket(s -> configure(s));
		RouterConfig routerConfig = new RouterConfig()
											.setMetaFile(metaFile )
											.setWebappOverrides(appOverrides)
											.setFileEncoding(CHAR_SET_TO_USE)
											.setDefaultResponseBodyEncoding(CHAR_SET_TO_USE)
											.setCachedCompressedDirectory(cacheDir)
											.setSecretKey(SecretKeyInfo.generateForTest());
		TemplateConfig templateConfig = new TemplateConfig();
		
		webServer = WebServerFactory.create(config, routerConfig, templateConfig);
	}

	public void configure(ServerSocketChannel channel) throws SocketException {
		channel.socket().setReuseAddress(true);
		//channel.socket().setSoTimeout(timeout);
		//channel.socket().setReceiveBufferSize(size);
	}
	
	public HttpRequestListener start() {
		return webServer.start();	
	}

	public void stop() {
		webServer.stop();
	}

	public TCPServerChannel getUnderlyingHttpChannel() {
		return webServer.getUnderlyingHttpChannel();
	}

}
