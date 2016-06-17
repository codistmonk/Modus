package modus.web;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;

/**
 * @author codistmonk (creation 2015-08-14)
 */
public abstract class Context implements Serializable {
	
	public abstract File getWorkingDirectory();
	
	public abstract void scmPush(String userId, String commitMessage, Collection<String> toCommit);
	
	public abstract void scmPush(String userId, String filePathInProject);
	
	public abstract void scmPull();
	
	public abstract void destroy();
	
	public abstract void log(String message);
	
	public abstract void log(String message, Throwable throwable);
	
	private static final long serialVersionUID = 2940398734434752907L;
	
	public static final void delete(final Path root) throws IOException {
		Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
			
			@Override
			public final FileVisitResult visitFile(final Path file,
					final BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				
				return super.visitFile(file, attrs);
			}
			
			@Override
			public final FileVisitResult postVisitDirectory(final Path dir,
					final IOException exc) throws IOException {
				Files.delete(dir);
				
				return super.postVisitDirectory(dir, exc);
			}
			
		});
	}
	
}
