package modus.web;

import static multij.tools.Tools.*;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import multij.tools.IllegalInstantiationException;
import multij.tools.Launcher;

/**
 * @author codistmonk (creation 2016-06-18)
 */
public final class AuthenticationTools {
	
	private AuthenticationTools() {
		throw new IllegalInstantiationException();
	}
	
	private static final Pattern CN_PATTERN = Pattern.compile(".*\\b[cC][nN]=([^,]+).*");
	
	public static final String getUserId(final HttpServletRequest request) {
		final X509Certificate[] certificates = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
		
		if (certificates != null && 0 < certificates.length) {
			final String dname = certificates[0].getSubjectDN().getName();
			final Matcher matcher = CN_PATTERN.matcher(dname);
			
			if (matcher.matches()) {
				return matcher.group(1);
			}
		}
		
		return null;
	}
	
	public static void importKeyStore(final String sourceStorePath, final String sourceStorePass,
			final String destinationStorePath, final String destinationStorePass)
			throws IOException, InterruptedException {
		final Process keytool = Runtime.getRuntime().exec(array(
				"keytool", "-importkeystore",
				"-srckeystore", sourceStorePath,
				"-destkeystore", destinationStorePath,
				"-deststorepass", destinationStorePass,
				"-srcstorepass", sourceStorePass));
		
		Launcher.pipe(keytool.getErrorStream(), System.err);
		
		debugPrint("keytool:", keytool.waitFor());
	}
	
	public static final void generatePFX(final String trustStorePath, final String trustStorePass, final String userId,
			final String userPass) throws IOException, InterruptedException {
		generateKey(trustStorePath, trustStorePass, userId, userPass);
		exportPFX(trustStorePath, trustStorePass, userId, userPass);
	}
	
	public static final void exportPFX(final String trustStorePath, final String trustStorePass, final String userId,
			final String userPass) throws IOException, InterruptedException {
		final Process keytool = Runtime.getRuntime().exec(array(
				"keytool", "-importkeystore",
				"-srckeystore", trustStorePath,
				"-srcalias", userId,
				"-destkeystore", userId + ".pfx",
				"-deststoretype", "PKCS12",
				"-deststorepass", userPass,
				"-srckeypass", userPass,
				"-srcstorepass", trustStorePass));
		
		Launcher.pipe(keytool.getErrorStream(), System.err);
		
		debugPrint("keytool:", keytool.waitFor());
	}
	
	public static void generateKey(final String trustStorePath, final String trustStorePass, final String userId,
			final String userPass) throws IOException, InterruptedException {
		final Process keytool = Runtime.getRuntime().exec(array(
				"keytool", "-genkey",
				"-keyalg", "RSA",
				"-alias", userId,
				"-keystore", trustStorePath,
				"-storepass", trustStorePass,
				"-keypass", userPass,
				"-validity", "360",
				"-keysize", "2048",
				"-dname", dname(userId)));
		
		Launcher.pipe(keytool.getErrorStream(), System.err);
		
		debugPrint("keytool:", keytool.waitFor());
	}
	
	public static final String dname(final String userId) {
		return "cn=" + userId + ", ou=Modus, o=Modus, c=XX";
	}
	
}
