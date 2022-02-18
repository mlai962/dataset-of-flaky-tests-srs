package main.java.flakyTestSearch;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public class TestFlakyness {
	public static int analyseSingleTestOutput(BufferedReader reader) {
		int returnValue = 1;
		String line;

		try {
			while ((line = reader.readLine()) != null) {
				System.out.println(line);
				if (line.contains("Failures: 1") || line.contains("Errors: 1")) {
					returnValue = 0;
				} else if (line.contains("BUILD FAILURE")) {
					returnValue = 2;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return returnValue;
	}

	// Method below taken from https://mkyong.com/java/how-to-execute-shell-command-from-java/
	public static int executeCommand(String cmd, String directory) {
		int returnValue = 1;
		int exitValue = 0;

		try {
			ProcessBuilder processBuilder = new ProcessBuilder();

			processBuilder.command("bash", "-l", "-c", cmd).directory(new File(directory));

			Process process = processBuilder.start();

			exitValue = process.waitFor();

			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			BufferedReader error = new BufferedReader(new InputStreamReader(process.getErrorStream()));

			returnValue = analyseSingleTestOutput(reader);

			String errorLine;
			while ((errorLine = error.readLine()) != null) {
				System.out.println(errorLine);
			}

			if (exitValue != 0) {
				process.destroy();

				System.out.println("command execution failure");

				returnValue = 2;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} 

		return returnValue;
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
			cmd = Config.MVN_DIR + " test -Dtest=" + className + "#" + testName;
		} else if (hasWrapper) {
			cmd = "./gradlew test --tests " + className + "." + testName + " -i";
		} else {
			cmd = "gradle test --tests " + className + "." + testName + " -i";
		}

		for (int i = 0; i < numOfRuns; i++) {
			int result = executeCommand(cmd, dir);

			if (result == 0) {
				failures++;
			} else if (result == 2) {
				return new TestResult(className, testName, 0, false, true);
			}
		}

		flakyness = failures / numOfRuns;

		return new TestResult(className, testName, flakyness, false, false);
	}
}
