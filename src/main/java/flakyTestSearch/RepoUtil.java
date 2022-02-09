package main.java.flakyTestSearch;

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
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.Node;

public class RepoUtil {
	private static String dir = Config.TEMP_DIR;
	private static File testClassDir;
	private static String repoName = "";
	private static String testClassName;
	private static boolean isTestClass = false;
	private static boolean isClass = false;;
	private static boolean hasTestName = false;
	private static boolean hasEither = false;
	private static boolean isMaven = false;
	private static boolean hasWrapper = false;

	public static void deleteTempRepoDir() {
		if (!repoName.equals("")) {
//			try {
//				FileUtils.delete(new File(dir + File.separator + repoName), 
//						FileUtils.RECURSIVE);
//				
//				FileUtils.delete(new File(dir + File.separator + repoName), 
//						FileUtils.RETRY);
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
			System.out.println(repoName);
			System.out.println("RD \"" + repoName + "\" /q /s");
			executeCommand("RD \"" + repoName + "\" /q /s", dir, false);
		}
	}

	public static void setUp(Project project, String testClass, boolean isPullRequest) {
		int repoNameLastSlash = project.getProjectName().lastIndexOf("/");
		repoName = project.getProjectName().substring(repoNameLastSlash+1, 
				project.getProjectName().length());

		if (isPullRequest) {
			int classNameLastSlash = testClass.lastIndexOf("/");
			testClassDir = new File(dir + File.separator + repoName + File.separator + 
					testClass.substring(0, classNameLastSlash+1) + File.separator);
	
			testClassName = testClass.substring(classNameLastSlash+1, 
					testClass.length());
		}
	}
	
	// Method below taken from https://mkyong.com/java/how-to-execute-shell-command-from-java/
	public static boolean executeCommand(String cmd, String directory, boolean enableTimeout) {
		boolean exitStatus = false;
		
		try {
			ProcessBuilder processBuilder = new ProcessBuilder();

			processBuilder.command("cmd.exe", "/c", cmd).directory(new File(directory));

			Process process = processBuilder.start();
			
			if (enableTimeout) {
				exitStatus = process.waitFor(Config.TIMEOUT_CLONING, TimeUnit.SECONDS);
			} else {
				process.waitFor();
			}
			
			
			
			if (!exitStatus) {
				process.destroy();

				executeCommand("taskkill /IM \"git-remote-https.exe\" /F", dir, false);
				
				process.waitFor();
				
				System.out.println("timeout");
				
				return exitStatus;
			}
			
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
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return exitStatus;
	}
	
	public static boolean cloneRepo (Project project) {
		String repoURL = project.getProjectName();
		String cloneURL = repoURL.replace("api.", "").replace("repos/", "") + ".git";

		String commitHash = project.getCommitHash();

		boolean isCloned = executeCommand("git clone " + cloneURL, dir, true);

		if (isCloned) {
			return executeCommand("git reset --hard " + commitHash, 
					dir + File.separator + repoName, true);
		} else {
			return false;
		}
	}

	public static boolean checkClassExists (Project project) {
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
	
	public static boolean[] checkWrapper() {
		// First index of return array indicates if pom.xml or build.gradle exists
		// Second index indicates if Maven (true) or Gradle (false)
		// Third index indicates if there is a wrapper (true) or not (false)
		
		hasEither = false;
		isMaven = false;
		hasWrapper = false;
		
		File repoDir = new File(dir + File.separator + repoName);
		
		File[] matches = repoDir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				if (name.contains("pom.xml")) {
					isMaven = true;
					hasEither = true;
				} else if (name.contains("build.gradle")) {
					isMaven = false;
					hasEither = true;
				}
				
				if (name.contains("mvnw.cmd") || name.contains("gradlew")) {
					hasWrapper = true;
				}
				
				return true;
			}
		});
		
		return new boolean[]{hasEither, isMaven, hasWrapper};
		
	}
	
	public static boolean checkCompile(boolean isMaven, boolean hasWrapper) {
		String repoDir = dir + File.separator + repoName;
		boolean builds = false;
		
		if (isMaven && hasWrapper) {
			builds = executeCommand("mvnw.cmd install -Dmaven.test.skip=true -Dmaven.javadoc.skip=true", repoDir, false);
		} else if (isMaven) {
			builds = executeCommand("mvn install -Dmaven.test.skip=true -Dmaven.javadoc.skip=true", repoDir, false);
		} else if (hasWrapper) {
			builds = executeCommand("gradlew build -x test", repoDir, false);
		} else {
			builds = executeCommand("gradle build -x test", repoDir, false);
		}
		
		System.out.println(builds);
		
		return builds;
	}
}
