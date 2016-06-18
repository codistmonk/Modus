package modus.web;

import static multij.tools.Tools.unchecked;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;

import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;

/**
 * @author codistmonk (creation 2016-08-15)
 */
public abstract class GitContext extends Context {
	
	private final boolean deleteRootOnDestroy;
	
	private final Path projectDataRoot;
	
	private final Git git;
	
	protected GitContext(final boolean deleteLocalDataOnDestroy, final String localGitRoot, final String remoteGitRoot, final String projectName) throws GitAPIException {
		this.deleteRootOnDestroy = deleteLocalDataOnDestroy;
		this.projectDataRoot = new File(localGitRoot).toPath();
		final File directory = new File(this.projectDataRoot.toFile(), projectName);
		Git git = null;
		
		try {
			git = Git.open(directory);
		} catch (final IOException exception) {
			this.log("Failed to open working directory", exception);
			
			final String uri = new File(remoteGitRoot, projectName + ".git").toURI().toString();
			
			this.log("Cloning " + uri + " to " + directory + "...");
			
			git = Git.cloneRepository()
					.setURI(uri)
					.setDirectory(directory)
					.call();
		}
		
		this.git = git;
		
		try {
			this.scmPull();
		} catch (final JGitInternalException exception) {
			if ("Could not get advertised Ref for branch master".equals(exception.getMessage())) {
				final File file = new File(this.getWorkingDirectory(), ".gitignore");
				
				try {
					file.createNewFile();
				} catch (final IOException exception1) {
					exception.printStackTrace();
				}
				
				this.scmPush(this.getClass().getName(), file.getPath());
				this.scmPull();
			} else {
				throw unchecked(exception);
			}
		}
	}
	
	public final Git getGit() {
		return this.git;
	}
	
	@Override
	public final File getWorkingDirectory() {
		return this.git.getRepository().getWorkTree();
	}
	
	@Override
	public final void scmPush(final String userId, final String commitMessage, final Collection<String> toCommit) {
		try {
			if (!toCommit.isEmpty()) {
				final AddCommand addCommand = this.scmAdd();
				
				toCommit.forEach(addCommand::addFilepattern);
				
				addCommand.call();
			}
			
			this.scmCommitAndPush(userId, commitMessage);
			
			toCommit.clear();
		} catch (final GitAPIException exception) {
			throw unchecked(exception);
		}
	}
	
	@Override
	public final void scmPush(final String userId, final String filePathInProject) {
		try {
			this.scmAdd().addFilepattern(filePathInProject).call();
			this.logGitStatus();
			this.scmCommitAndPush(userId, "Update " + filePathInProject);
		} catch (final GitAPIException exception) {
			throw unchecked(exception);
		}
	}
	
	@Override
	public final void scmPull() {
		try {
			this.git.pull().call();
		} catch (final GitAPIException exception) {
			throw unchecked(exception);
		}
	}
	
	@Override
	public final void destroy() {
		this.git.close();
		
		if (this.deleteRootOnDestroy) {
			try {
				delete(this.projectDataRoot);
			} catch (final Exception exception) {
				this.log("Error", exception);
			}
		}
	}
	
	public final AddCommand scmAdd() {
		return this.git.add();
	}
	
	public final void scmCommitAndPush(final String userId, final String message) {
		final String host = "modusservlet";
		
		try {
			this.logGitStatus();
			this.git.commit()
				.setCommitter(userId, userId + "@" + host)
				.setAuthor(userId, userId + "@" + host)
				.setMessage(message)
				.call();
			this.git.push().call();
		} catch (final GitAPIException exception) {
			throw unchecked(exception);
		}
	}
	
	public final void logGitStatus() throws GitAPIException {
		this.log("added: " + this.git.status().call().getAdded().toString());
		this.log("modified: " + this.git.status().call().getModified().toString());
		this.log("changed: " + this.git.status().call().getChanged().toString());
		this.log("untracked: " + this.git.status().call().getUntracked().toString());
		this.log("uncommitted: " + this.git.status().call().getUncommittedChanges().toString());
	}
	
	private static final long serialVersionUID = -8329596544104661827L;
	
}