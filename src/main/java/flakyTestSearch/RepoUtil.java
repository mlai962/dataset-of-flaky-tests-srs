package flakyTestSearch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import org.eclipse.jgit.util.FileUtils;

public class RepoUtil {
	private static String dir = "C:\\Users\\Matthew\\Downloads\\Documents\\School 2021\\tempRepo";
	private static String lastRepoName = "";

	// ****** https://mkyong.com/java/how-to-execute-shell-command-from-java/ ***** 
	public static boolean executeCommand(String cmd, String directory) {
		boolean isExecuted = false;

		try {
			ProcessBuilder processBuilder = new ProcessBuilder();

			processBuilder.command("cmd.exe", "/c", cmd).directory(new File(directory));

			Process process = processBuilder.start();

			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			BufferedReader error = new BufferedReader(new InputStreamReader(process.getErrorStream()));

			String line;
			while ((line = reader.readLine()) != null) {
				System.out.println(line);
			}

			String errorLine;
			while ((errorLine = error.readLine()) != null) {
				System.out.print(errorLine);
			}

			int exitVal = process.waitFor();
			if (exitVal == 0) {
				isExecuted = true;
				System.out.println("Success!");
			} else {
				System.out.println("Fail!");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return isExecuted;
	}

	public static void deleteTempRepoDir() {
		try {
			FileUtils.delete(new File(dir + File.separator + lastRepoName + File.separator), 
					org.eclipse.jgit.util.FileUtils.RECURSIVE);
		} catch (java.io.IOException e) {
			e.printStackTrace();
		}
	}

	public static boolean cloneRepo (Project project) {
		if (!lastRepoName.equals("")) {
			deleteTempRepoDir();
		}

		String repoURL = project.getProjectName();
		String cloneURL = repoURL.replace("api.", "").replace("repos/", "") + ".git/";

		String commitHash = project.getCommitHash();

		boolean isCloned = executeCommand("git clone " + cloneURL, dir);

		if (isCloned) {
			int lastSlash = project.getProjectName().lastIndexOf("/");
			String repoName = project.getProjectName().substring(lastSlash+1, 
					project.getProjectName().length());

			lastRepoName = repoName;

			return executeCommand("git reset " + commitHash + " --hard", 
					dir + File.separator + repoName);
		} else {
			return false;
		}
	}

	public static Boolean checkClassExists (Project project) {
		int repoNameLastSlash = project.getProjectName().lastIndexOf("/");
		String repoName = project.getProjectName().substring(repoNameLastSlash+1, 
				project.getProjectName().length());

		int classNameLastSlash = project.getClassName().lastIndexOf("/");
		File testClassDir = new File(dir + File.separator + repoName + File.separator + 
				project.getClassName().substring(0, classNameLastSlash+1) + File.separator);

		final String testClassName = project.getClassName().substring(classNameLastSlash+1, 
				project.getClassName().length());

		System.out.println(testClassName);
		System.out.println(testClassDir);

		File[] matches = testClassDir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.startsWith(testClassName);
			}
		});

		if (!(matches == null)) {
			System.out.println(matches[0]);
			return true;
		}

		return false;
	}
}
