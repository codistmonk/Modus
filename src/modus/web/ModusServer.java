package modus.web;

import static multij.tools.Tools.*;

import java.io.File;

import multij.tools.CommandLineArgumentsParser;
import multij.tools.IllegalInstantiationException;
import multij.tools.Launcher;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;

/**
 * @author codistmonk (creation 2016-06-05)
 */
public final class ModusServer {
	
	private ModusServer() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 * @throws Exception 
	 */
	public static final void main(final String... commandLineArguments) throws Exception {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final Server server = new Server();
		final int port = arguments.get1("port", 1443);
		final SslContextFactory sslContextFactory = new SslContextFactory();
		final String keystorePath = arguments.get("keystore", "keystore.jks");
		final String keystorePass = arguments.get("keystorePass", "YJ2tZvFT");
//		final String trustStorePath = arguments.get("truststore", "truststore.jks");
//		final String trustStorePass = arguments.get("truststorePass", keystorePass);
		final File keystoreFile = new File(keystorePath);
		final String localGitRoot = arguments.get("localGitRoot", ".modusservlet/git");
		final String remoteGitRoot = arguments.get("remoteGitRoot", System.getProperty("user.home") + "/git");
		
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
		
		sslContextFactory.setKeyStorePath(keystorePath);
		sslContextFactory.setKeyStorePassword(keystorePass);
//		sslContextFactory.setTrustStorePath(trustStorePath);
//		sslContextFactory.setTrustStorePassword(trustStorePass);
		
		final ServerConnector https = new ServerConnector(server, sslContextFactory);
		
		https.setPort(port);
		
		server.setConnectors(array(https));
		
		final GzipHandler gzip = new GzipHandler();
		
		server.setHandler(gzip);
		
		final HandlerList handlers = new HandlerList();
		
		final ResourceHandler resourceHandler = new ResourceHandler();
		
		resourceHandler.setDirectoriesListed(true);
		resourceHandler.setWelcomeFiles(array("index.html"));
		resourceHandler.setResourceBase("src/modus/web/templates");
		
		handlers.addHandler(resourceHandler);
		handlers.addHandler(new ModusHandler(localGitRoot, remoteGitRoot));
		
		gzip.setHandler(handlers);
		
		server.start();
		server.dumpStdErr();
		server.join();
	}
	
}