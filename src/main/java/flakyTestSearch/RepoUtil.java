package flakyTestSearch;

import java.io.File;
import java.nio.file.Paths;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.util.FileUtils;

public class RepoUtil {
	public static void deleteTempRepoDir() {
		String dir = "C:\\Users\\Matthew\\Downloads\\Documents\\School 2021\\tempRepo";
		try {
			FileUtils.delete(new File(dir), org.eclipse.jgit.util.FileUtils.RECURSIVE);
		} catch (java.io.IOException e) {
			e.printStackTrace();
		}
	}

	public static Boolean cloneRepo (Project project) {
		Boolean isCloned = false;

		String repoURL = project.getProjectName();
		String directoryPath = "C:\\Users\\Matthew\\Downloads\\Documents\\School 2021\\tempRepo";
		
		try {
		    Git.cloneRepository()
		        .setURI(repoURL)
		        .setDirectory(Paths.get(directoryPath).toFile())
		        .call();
		    
		    isCloned = true;
		} catch (GitAPIException e) {
		    System.out.println("Exception occurred while cloning repo");
		    e.printStackTrace();
		}
		
		return isCloned;
	}
}
