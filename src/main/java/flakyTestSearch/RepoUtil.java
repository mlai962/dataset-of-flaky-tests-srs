package main.java.flakyTestSearch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

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
			System.out.println(repoName);
			executeCommand("rm -rf " + repoName, dir);
		}
	}

	public static void setUp(Project project, String testClass, boolean isPullRequest) {
		int repoNameLastSlash = project.getProjectURL().lastIndexOf("/");
		repoName = project.getProjectURL().substring(repoNameLastSlash+1, 
				project.getProjectURL().length());
		
		project.setProjectName(repoName);

		if (isPullRequest) {
			int classNameLastSlash = testClass.lastIndexOf("/");
			
			testClassDir = new File(dir + File.separator + repoName + File.separator + 
					testClass.substring(0, classNameLastSlash+1) + File.separator);
	
			testClassName = testClass.substring(classNameLastSlash+1, 
					testClass.length());
		}
	}
	
	// Method below taken from https://mkyong.com/java/how-to-execute-shell-command-from-java/
	public static boolean executeCommand(String cmd, String directory) {
		boolean exitStatus = false;
		
		try {
			ProcessBuilder processBuilder = new ProcessBuilder();

			processBuilder.command("sh", "-c", cmd).directory(new File(directory));

			Process process = processBuilder.start();
			
			exitStatus = process.waitFor(Config.TIMEOUT_CLONING, TimeUnit.SECONDS);
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			BufferedReader error = new BufferedReader(new InputStreamReader(process.getErrorStream()));

			boolean buildSuccess = true;
			
			String line;
			while ((line = reader.readLine()) != null) {
				System.out.println(line);
				
				if (line.contains("BUILD SUCCESS")) {
					buildSuccess = true;
				} else if (line.contains("BUILD FAIL") || line.contains("fatal: Could not parse object")) {
					buildSuccess = false;
				}
			}

			String errorLine;
			while ((errorLine = error.readLine()) != null) {
				System.out.println(errorLine);
				
				if (errorLine.contains("fatal: Could not parse object")) {
					buildSuccess = false;
				}
			}
			
			if (!exitStatus) {
				process.destroy();
				
				System.out.println("timeout");
				
				if (buildSuccess) {
					return true;
				}
				
				return exitStatus;
			}
			
			if (buildSuccess) {
				System.out.println("Build success");
				return true;
			} else if (!buildSuccess) {
				System.out.println("Build failure");
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("EXCEPTION");
			return false;
		}

		return exitStatus;
	}
	
	public static boolean cloneRepo (Project project) {
		String repoURL = project.getProjectURL();
		String cloneURL = repoURL.replace("api.", "").replace("repos/", "");
		
		System.out.println(cloneURL);

		String commitHash = project.getCommitHash();

		boolean isCloned = executeCommand("git clone " + cloneURL, dir);

		if (isCloned) {
			return executeCommand("git reset --hard " + commitHash, 
					dir + File.separator + project.getProjectName());
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
	public static List<String> findTestName(Project project, ArrayList<String> lineNums) {
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
			List<String> methodNames = new ArrayList<>();
			
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
									
									String anyMethod = n.getNameAsString();
									methodNames.add(anyMethod);
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
			
			project.setAllTestNames(testClassName, methodNames);
			
			if (!testNames.isEmpty()) {
				finalTestNames.add(testNames.get(testNames.size()-1));
			}
		}
		
		return finalTestNames;
    }
	
	public static boolean[] checkIssueClassExistsAndIfTestClass(Project project, String testClass, String testName) {
		long startTime = System.currentTimeMillis();
		List<String> methodNames = new ArrayList<>();
		
		try {
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
					
					try {
						new VoidVisitorAdapter<Object>() {
							@Override
							public void visit(MethodDeclaration n, Object arg) {
								super.visit(n, arg);
								String anyMethod = n.getNameAsString();
								methodNames.add(anyMethod);
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
			}).explore(new File(dir));
		} catch (com.github.javaparser.ParseProblemException e) {
			return new boolean[]{isClass, isTestClass, hasTestName};
		}
		
		project.setAllTestNames(testClass, methodNames);
		
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
		
		@SuppressWarnings("unused")
		File[] matches = repoDir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				if (name.contains("pom.xml")) {
					isMaven = true;
					hasEither = true;
				} else if (name.contains("build.gradle")) {
					isMaven = false;
					hasEither = true;
				}
				
				if (name.contains("mvnw") || name.contains("gradlew")) {
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
			builds = executeCommand("./mvnw clean install -Dmaven.test.skip=true -Dmaven.javadoc.skip=true", repoDir);
		} else if (isMaven) {
			builds = executeCommand(Config.MVN_DIR + " clean install -Dmaven.test.skip=true -Dmaven.javadoc.skip=true", repoDir);
		} else if (hasWrapper) {
			builds = executeCommand("./gradlew clean build -x test -x javadoc", repoDir);
		} else {
			builds = executeCommand("gradle clean build -x test -x javadoc", repoDir);
		}
		
		System.out.println(builds);
		
		return builds;
	}
}
