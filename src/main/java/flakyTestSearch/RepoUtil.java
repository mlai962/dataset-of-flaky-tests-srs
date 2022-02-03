package flakyTestSearch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

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
	private static boolean isClass = false;;
	private static boolean hasTestName = false;

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
				System.out.println(errorLine);
			}
			
			process.waitFor(Config.TIMEOUT_CLONING, TimeUnit.SECONDS);
			
			process.destroy();

			int exitVal = process.waitFor();
			if (exitVal == 0) {
				isExecuted = true;
			} else {
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

	public static boolean checkClassExists (Project project, String testClass) {
		int repoNameLastSlash = project.getProjectName().lastIndexOf("/");
		repoName = project.getProjectName().substring(repoNameLastSlash+1, 
				project.getProjectName().length());

		int classNameLastSlash = testClass.lastIndexOf("/");
		testClassDir = new File(dir + File.separator + repoName + File.separator + 
				testClass.substring(0, classNameLastSlash+1) + File.separator);

		testClassName = testClass.substring(classNameLastSlash+1, 
				testClass.length());

		File[] matches = testClassDir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.startsWith(testClassName);
			}
		});

		if (!(matches == null)) {
			return true;
		}

		return false;
	}
	
	// Main code taken from https://tomassetti.me/getting-started-with-javaparser-analyzing-java-code-programmatically/
	public static ArrayList<String> findTestName(Project project, ArrayList<String> lineNums) {
		ArrayList<String> testNames = new ArrayList<>();
		ArrayList<String> finalTestNames = new ArrayList<>();

		long startTime = System.currentTimeMillis();
		
		new DirExplorer((level, path, file) -> path.endsWith(".java"), (level, path, file) -> {
			if (path.contains(testClassName)) {
				try {
					new NodeIterator(new NodeIterator.NodeHandler() {
						@Override
						public boolean handle(Node node) {
							if(node.toString().contains("@Test")) {
								isTestClass = true;
								return true;
							}
							
							return false;
						}
					}).explore(StaticJavaParser.parse(file));
				} catch (IOException e) {
					new RuntimeException(e);
				}
			}
			
			long elapsedTime = System.currentTimeMillis() - startTime;
			if (elapsedTime > Config.TIMEOUT_SEARCHING) {
				System.out.println("timeout");
				return;
			}
		}).explore(testClassDir);

		for (String num : lineNums) {
			int changedLine = Integer.parseInt(num);
			
			if (isTestClass) {
				new DirExplorer((level, path, file) -> path.endsWith(".java"), (level, path, file) -> {
					if (path.contains(testClassName)) {
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
						} catch (IOException e) {
							new RuntimeException(e);
						}
					}
					
					long elapsedTime = System.currentTimeMillis() - startTime;
					if (elapsedTime > Config.TIMEOUT_SEARCHING) {
						System.out.println("timeout");
						return;
					}
				}).explore(testClassDir);
			}
			
			if (!testNames.isEmpty()) {
				finalTestNames.add(testNames.get(testNames.size()-1));
			}
		}
		
		return finalTestNames;
    }
	
	public static boolean[] checkIssueClassExistsAndIfTestClass(Project project, String testClass, String testName) {
		int repoNameLastSlash = project.getProjectName().lastIndexOf("/");
		repoName = project.getProjectName().substring(repoNameLastSlash+1, 
				project.getProjectName().length());
		
		long startTime = System.currentTimeMillis();
		
		new DirExplorer((level, path, file) -> path.endsWith(".java"), (level, path, file) -> {
			if (path.contains(testClass)) {
				testClassDir = new File(dir + path);
				isClass = true;
				
				try {
					new NodeIterator(new NodeIterator.NodeHandler() {
						@Override
						public boolean handle(Node node) {
							if (node.toString().contains("@Test")) {
								isTestClass = true;
							}
							
							if (node.toString().contains(testName)) {
								hasTestName = true;
							}
							return false;
						}
					}).explore(StaticJavaParser.parse(file));
				} catch (IOException e) {
					new RuntimeException(e);
				}
			}
			
			long elapsedTime = System.currentTimeMillis() - startTime;
			if (elapsedTime > Config.TIMEOUT_SEARCHING) {
				System.out.println("timeout");
				return;
			}
		}).explore(new File(dir));
		
		return new boolean[]{isClass, isTestClass, hasTestName};
	}
}
