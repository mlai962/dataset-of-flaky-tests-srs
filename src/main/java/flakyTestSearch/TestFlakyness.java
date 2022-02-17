package main.java.flakyTestSearch;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public class TestFlakyness {
	// Method below taken from https://mkyong.com/java/how-to-execute-shell-command-from-java/
		public static boolean executeCommand(String cmd, String directory) {
			int exitValue = 0;
			
			try {
				ProcessBuilder processBuilder = new ProcessBuilder();

				processBuilder.command("bash", "-l", "-c", cmd).directory(new File(directory));

				Process process = processBuilder.start();
				
				exitValue = process.waitFor();
				
				BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
				BufferedReader error = new BufferedReader(new InputStreamReader(process.getErrorStream()));

				String line;
				while ((line = reader.readLine()) != null) {
					System.out.println(line);
					if (line.contains("BUILD SUCCESS")) {
						return true;
					}
					
				}

				String errorLine;
				while ((errorLine = error.readLine()) != null) {
					System.out.println(errorLine);
				}
				
				if (exitValue != 0) {
					process.destroy();
					
					System.out.println("command execution failure");
					
					return false;
				} else {
					return true;
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			return false;
		}
	
	public static TestResult runSingleTest(Project project, String className, String testName, int numOfRuns, boolean isMaven, boolean hasWrapper) {
		double failures = 0;
		double flakyness = 0;
		
		int lastSlash = className.lastIndexOf("/");
		className = className.substring(lastSlash+1, className.length());
		
		String dir = Config.TEMP_DIR + File.separator + project.getProjectName();
		String cmd = "";
		
		if (isMaven && hasWrapper) {
			cmd = "./mvnw test -Dtest=" + className + "#" + testName;
		} else if (isMaven) {
			cmd = "mvn test -Dtest=" + className + "#" + testName;
		} else if (hasWrapper) {
			cmd = "./gradlew test --tests " + className + "." + testName + " -i";
		} else {
			cmd = "gradle test --tests " + className + "." + testName + " -i";
		}
		
		for (int i = 0; i < numOfRuns; i++) {
			boolean testPassed = executeCommand(cmd, dir);
			
			if (!testPassed) {
				failures++;
			}
		}
		
		flakyness = failures / numOfRuns;
		
		return new TestResult(className, testName, flakyness, false);
	}
}
