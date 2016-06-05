package modus.web;

import static multij.tools.Tools.array;
import static multij.tools.Tools.debugPrint;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import multij.tools.IllegalInstantiationException;
import multij.tools.Launcher;

/**
 * @author codistmonk (creation 2016-06-05)
 */
public final class ModusServer {
	
	private ModusServer() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Unused
	 * @throws Exception 
	 */
	public static final void main(final String... commandLineArguments) throws Exception {
		final Server server = new Server();
		final SslContextFactory sslContextFactory = new SslContextFactory();
		final File keystoreFile = new File("keystore.jks");
		final String keystorePass = "YJ2tZvFT";
		
		if (!keystoreFile.exists()) {
			final Process keytool = Runtime.getRuntime().exec(array(
					"keytool", "-genkey",
					"-keyalg", "RSA",
					"-alias", "selfsigned",
					"-keystore", "keystore.jks",
					"-storepass", keystorePass,
					"-keypass", keystorePass,
					"-validity", "360",
					"-keysize", "2048",
					"-dname", "cn=Modus, ou=Modus, o=Modus, c=XX"));
			
			Launcher.pipe(keytool.getErrorStream(), System.err);
			
			debugPrint("keytool:", keytool.waitFor());
		}
		
		sslContextFactory.setKeyStorePath(keystoreFile.getPath());
		sslContextFactory.setKeyStorePassword(keystorePass);
		
		final ServerConnector https = new ServerConnector(server, sslContextFactory);
		
		https.setPort(1443);
		
		server.setConnectors(array(https));
		
		final GzipHandler gzip = new GzipHandler();
		
		server.setHandler(gzip);
		
		final HandlerList handlers = new HandlerList();
		
		final ResourceHandler resourceHandler = new ResourceHandler();
		
		resourceHandler.setDirectoriesListed(true);
		resourceHandler.setWelcomeFiles(array("index.html"));
		resourceHandler.setResourceBase("src/modus/web/templates");
		
		handlers.addHandler(resourceHandler);
		handlers.addHandler(new ModusHandler());
		
		gzip.setHandler(handlers);
		
		server.start();
		server.dumpStdErr();
		server.join();
	}
	
}

/**
 * @author codistmonk (creation 2016-06-05)
 */
final class ModusHandler extends AbstractHandler {
	
	@Override
	public final void handle(final String target, final Request baseRequest, final HttpServletRequest request, final HttpServletResponse response)
			throws IOException, ServletException {
		debugPrint(target);
		response.setContentType("text/html");
		response.setStatus(HttpServletResponse.SC_OK);
		
		final PrintWriter out = response.getWriter();
		
		out.println("<h1>hi</h1>");
		
		baseRequest.setHandled(true);
	}
	
}