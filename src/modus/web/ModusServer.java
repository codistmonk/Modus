package modus.web;

import static modus.web.AuthenticationTools.*;
import static multij.tools.Tools.*;

import java.io.File;

import multij.tools.CommandLineArgumentsParser;
import multij.tools.IllegalInstantiationException;

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
		final String keyStorePath = arguments.get("keystore", "keystore.jks");
		final String keyStorePass = arguments.get("keystorepass", "YJ2tZvFT");
		final String trustStorePath = arguments.get("truststore", "truststore.jks");
		final String trustStorePass = arguments.get("truststorepass", keyStorePass);
		final File keyStoreFile = new File(keyStorePath);
		final File trustStoreFile = new File(trustStorePath);
		final String localGitRoot = arguments.get("localgit", ".modusservlet/git");
		final String remoteGitRoot = arguments.get("remotegit", System.getProperty("user.home") + "/git");
		
		if (!keyStoreFile.exists()) {
			generateKey(keyStorePath, keyStorePass, "Modus", keyStorePass);
		}
		
		sslContextFactory.setKeyStorePath(keyStorePath);
		sslContextFactory.setKeyStorePassword(keyStorePass);
		
		if (!trustStoreFile.exists()) {
			importKeyStore(keyStorePath, keyStorePass, trustStorePath, trustStorePass);
			
			final String userId = "cm";
			
			generatePFX(trustStorePath, trustStorePass, userId, userId + "-modus");
		}
		
		sslContextFactory.setTrustStorePath(trustStorePath);
		sslContextFactory.setTrustStorePassword(trustStorePass);
		
		sslContextFactory.setWantClientAuth(true);
		
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
