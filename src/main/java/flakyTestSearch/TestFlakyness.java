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

				processBuilder.command("sh", "-c", cmd).directory(new File(directory));

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
	
	public static TestResult RunSingleTest(Project project, String className, String testName, int numOfRuns) {
		double failures = 0;
		double flakyness = 0;
		
		int lastSlash = className.lastIndexOf("/");
		className = className.substring(lastSlash+1, className.length());
		
		String cmd = "mvn clean test -Dtest=" + className + "#" + testName;
		String dir = Config.TEMP_DIR + File.separator + project.getProjectName();
		
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
