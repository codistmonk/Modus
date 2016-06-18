package modus.web;

import static modus.web.AuthenticationTools.getUserId;
import static multij.tools.Tools.debugPrint;
import static multij.tools.Tools.unchecked;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.cert.X509Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import multij.xml.XMLTools;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoRemoteRepositoryException;
import org.w3c.dom.Document;

/**
 * @author codistmonk (creation 2016-06-05)
 */
final class ModusHandler extends AbstractHandler {
	
	private final Logger logger;
	
	private final GitContext context;
	
	public ModusHandler(final String localGitRoot, final String remoteGitRoot) {
		this.logger = Log.getLogger(this.getClass());
		GitContext context = null;
		
		try {
			context = this.newContext(localGitRoot, remoteGitRoot);
		} catch (final Exception exception) {
			final NoRemoteRepositoryException nrre = findCause(exception, NoRemoteRepositoryException.class);
			
			if (nrre != null) {
				final Pattern fileNotFoundPattern = Pattern.compile("file://(.+): not found.");
				final Matcher matcher = fileNotFoundPattern.matcher(nrre.getMessage());
				
				if (matcher.matches()) {
					final File f = new File(matcher.group(1));
					
					log("remote: " + f);
					log("mkdirs: " + f.mkdirs());
					
					if (f.isDirectory()) {
						try {
							log("git init --bare " + f);
							
							Git.init().setDirectory(f).setBare(true).call();
							
							context = this.newContext(localGitRoot, remoteGitRoot);
						} catch (final Exception exception1) {
							throw unchecked(exception1);
						}
					}
				}
			} else {
				throw unchecked(exception);
			}
		}
		
		this.context = context;
	}
	
	public final void log(final String message, final Throwable throwable) {
		this.logger.warn(message, throwable);
	}
	
	public final void log(final String message) {
		this.logger.info(message);
	}
	
	@SuppressWarnings("unchecked")
	public static final <T extends Throwable> T findCause(final Throwable t, final Class<T> type) {
		if (type.isInstance(t)) {
			return (T) t;
		}
		
		if (t == null || t.getCause() == t) {
			return null;
		}
		
		return findCause(t.getCause(), type);
	}
	
	@Override
	public final void handle(final String target, final Request baseRequest, final HttpServletRequest request, final HttpServletResponse response)
			throws IOException, ServletException {
		final String userId = getUserId(request);
		
		debugPrint("userId:", userId);
		
		response.setContentType("text/html");
		
		final PrintWriter out = response.getWriter();
		Throwable issue = null;
		
		if (target.startsWith("/database/post/")) {
			try {
				final Document xml = XMLTools.parse(request.getParameter("xml"));
				final String moduleName = XMLTools.getString(xml, "/object/@name");
				
				debugPrint(moduleName);
				// TODO
				
				response.setStatus(HttpServletResponse.SC_OK);
				
				out.println("ok");
				
				baseRequest.setHandled(true);
				
				return;
			} catch (final Throwable exception) {
				this.log("error", exception);
				issue = exception;
				exception.printStackTrace();
			}
		}
		
		{
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			
			out.println("error: " + issue);
			
			baseRequest.setHandled(true);
		}
	}
	
	private GitContext newContext(final String localGitRoot, final String remoteGitRoot) throws GitAPIException {
		return new GitContext(false, localGitRoot, remoteGitRoot, "ModusData") {
			
			@Override
			public final void log(final String message, final Throwable throwable) {
				ModusHandler.this.log(message, throwable);
			}
			
			@Override
			public final void log(final String message) {
				ModusHandler.this.log(message);
			}
			
			private static final long serialVersionUID = 7872003944832901702L;
			
		};
	}
	
}