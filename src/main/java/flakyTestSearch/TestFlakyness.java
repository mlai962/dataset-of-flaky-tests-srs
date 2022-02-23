package main.java.flakyTestSearch;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TestFlakyness {
	// Method below taken from https://mkyong.com/java/how-to-execute-shell-command-from-java/
	public static int executeCommand(String cmd, String directory, String testName, boolean isSingleTest) {
		int returnValue = 1;
		int exitValue = 0;
		
		if (!isSingleTest) {
			int index;
			if (testName.contains("+")) {
				index = testName.indexOf("+");
			} else {
				index = testName.indexOf("-")-2;
			}

			testName = testName.substring(0, index+1);
		}

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
				if (line.contains("Failures: 1") || line.contains("Errors: 1")) {
					if (isSingleTest || (!isSingleTest && line.contains(testName))) {
						returnValue = 0;
					}
				} else if (line.contains("BUILD FAILURE")) {
					returnValue = 2;
				}
			}

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

		if (testName.contains(".") || testName.contains("+")) {
			int result = executeCommand(cmd, dir, testName, false);

			if (result == 0) {
				failures++;
			} else if (result == 2) {
				return new TestResult(className, testName, 0, false, true);
			}
		} else {
			for (int i = 0; i < numOfRuns; i++) {
				int result = executeCommand(cmd, dir, testName, true);

				if (result == 0) {
					failures++;
				} else if (result == 2) {
					return new TestResult(className, testName, 0, false, true);
				}
			}
		}

		flakyness = failures / numOfRuns;

		return new TestResult(className, testName, flakyness, false, false);
	}
	
	public static List<List<String>> getOrders(Project project, String className, String testName, int numOrders) {
		List<List<String>> orders = new ArrayList<>();
		
		List<String> testNames = project.getAllTestNames().get(className);
		int numTests = testNames.size();
		
		if (numTests > 4) {
			numTests = 4;
		}
		
		List<String> order = new ArrayList<>();
		
		for (int i = 0; i < numOrders; i++) {
			while(!order.contains(testName)) {
				for (int j = 0; j < numTests; j++) {
					order.add(testNames.get(j));
				}
				
				Collections.shuffle(testNames);
			}
			
			orders.add(order);
		}
		
		return orders;
	}
	
	public static TestResult runMultipleTests(Project project, String className, String testName, int numOfRuns, boolean isMaven, boolean hasWrapper) {
		double failures = 0;
		double flakyness = 0;
		
		String cmd = "";
		
		List<List<String>> orders = getOrders(project, className, testName, numOfRuns);
		List<String> order;
		
		TestResult testResult;
		
		for (int i = 0; i < numOfRuns; i++) {
			order = orders.get(i);
			boolean first = true;
			
			for (int j = 0; j < order.size(); j++) {
				if (isMaven) {
					if (first) {
						cmd = order.get(j);
					} else {
						first = false;
						cmd = cmd + "+" + order.get(j);
					}
				} else {
					if (first) {
						cmd = order.get(j);
					} else {
						first = false;
						cmd = cmd + " --tests " + className + "." + order.get(j);
					}
 				}
			}
			
			testResult = runSingleTest(project, className, cmd, 1, isMaven, hasWrapper);
			
			if (testResult.getIfTestFailCompile()) {
				return new TestResult(className, testName, 0, true, true);
			} else if (testResult.getFlakyness() == 0) {
				failures++;
			} 
		}
		
		flakyness = failures / numOfRuns;
		
		return new TestResult(className, testName, flakyness, true, false);
	}
}
