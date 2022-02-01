package flakyTestSearch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.eclipse.jgit.util.FileUtils;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.google.common.base.Strings;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.Node;

public class RepoUtil {
	private static String dir = "C:\\Users\\Matthew\\Downloads\\Documents\\School 2021\\tempRepo";
	private static String lastRepoName = "";
	private static File testClassDir;
	private static String repoName;
	private static String testClassName;
	private static boolean isTestClass = false;

	// Method below taken from https://mkyong.com/java/how-to-execute-shell-command-from-java/
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
		
		int lastSlash = project.getProjectName().lastIndexOf("/");
		String repoName = project.getProjectName().substring(lastSlash+1, 
				project.getProjectName().length());
		
		lastRepoName = repoName;

		if (isCloned) {
			return executeCommand("git reset " + commitHash + " --hard", 
					dir + File.separator + repoName);
		} else {
			return false;
		}
	}

	public static boolean checkClassExists (Project project) {
		int repoNameLastSlash = project.getProjectName().lastIndexOf("/");
		repoName = project.getProjectName().substring(repoNameLastSlash+1, 
				project.getProjectName().length());

		int classNameLastSlash = project.getClassName().lastIndexOf("/");
		testClassDir = new File(dir + File.separator + repoName + File.separator + 
				project.getClassName().substring(0, classNameLastSlash+1) + File.separator);

		testClassName = project.getClassName().substring(classNameLastSlash+1, 
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
	
	// Main code taken from https://tomassetti.me/getting-started-with-javaparser-analyzing-java-code-programmatically/
	public static ArrayList<String> findTestName(Project project, String lineNum) {
		ArrayList<String> testNames = new ArrayList<>();

		int changedLine = Integer.parseInt(lineNum);
		System.out.println(changedLine);

		new DirExplorer((level, path, file) -> path.endsWith(".java"), (level, path, file) -> {
			if (path.contains(testClassName)) {
				System.out.println(path);
				System.out.println(Strings.repeat("=", path.length()));
				try {
					new NodeIterator(new NodeIterator.NodeHandler() {
						@Override
						public boolean handle(Node node) {
							if(node.toString().contains("@Test")) {
								System.out.println("is a test class");
								isTestClass = true;
								return true;
							}
							return false;
						}
					}).explore(StaticJavaParser.parse(file));
					System.out.println(); // empty line
				} catch (IOException e) {
					new RuntimeException(e);
				}
			}
		}).explore(testClassDir);

		if (isTestClass) {
			new DirExplorer((level, path, file) -> path.endsWith(".java"), (level, path, file) -> {
				if (path.contains(testClassName)) {
					System.out.println(path);
					System.out.println(Strings.repeat("=", path.length()));
					try {
						new VoidVisitorAdapter<Object>() {
							@Override
							public void visit(MethodDeclaration n, Object arg) {
								super.visit(n, arg);
								int currentLine = n.getRange().get().begin.line;
								String method = null;
								if (currentLine < changedLine) {
									method = n.getNameAsString();
								} 
								if (!(method == null)) {
									testNames.add(method);
								}
							}
						}.visit(StaticJavaParser.parse(file), null);
						System.out.println(); // empty line
					} catch (IOException e) {
						new RuntimeException(e);
					}
				}
			}).explore(testClassDir);
		}
		
		return testNames;
    }
	
	public static boolean checkIssueClassExists(Project project) {
		int repoNameLastSlash = project.getProjectName().lastIndexOf("/");
		repoName = project.getProjectName().substring(repoNameLastSlash+1, 
				project.getProjectName().length());
		
		new DirExplorer((level, path, file) -> path.endsWith(".java"), (level, path, file) -> {
			if (path.contains(project.getClassName())) {
				System.out.println(path);
				System.out.println(Strings.repeat("=", path.length()));
				testClassDir = new File(dir + path);
				System.out.println(testClassDir);
				isTestClass = true;
				
			}
		}).explore(new File(dir));
		
		return isTestClass;
	}
}
