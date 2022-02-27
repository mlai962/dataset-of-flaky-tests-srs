package main.java.flakyTestSearch;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TestFlakyness {
	public static String multiTestName = "";
	
	// Method below taken from https://mkyong.com/java/how-to-execute-shell-command-from-java/
	public static int executeCommand(String cmd, String directory, String testName, boolean isSingleTest) {
		int returnValue = 1;
		boolean exitValue = false;
		
		if (!isSingleTest) {
			testName = multiTestName;
		}

		System.out.println(cmd);
		System.out.println(testName);
		System.out.println(isSingleTest);
		
		try {
			ProcessBuilder processBuilder = new ProcessBuilder();

			processBuilder.command("bash", "-l", "-c", cmd).directory(new File(directory));

			Process process = processBuilder.start();

			exitValue = process.waitFor(Config.TIMEOUT_CLONING, TimeUnit.SECONDS);

			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			BufferedReader error = new BufferedReader(new InputStreamReader(process.getErrorStream()));

			String line;
			
			while ((line = reader.readLine()) != null) {
				System.out.println(line);
				
				boolean mavenTestFailure = line.contains(testName + "(") && line.contains("<<< FAILURE!");
				boolean mavenTestError = line.contains(testName + "(") && line.contains("<<< ERROR!");
				boolean gradleTestFailure = line.contains(testName + "()" + " FAILED");
				boolean gradleTestError = line.contains(testName + "()" + " ERROR");

				if (mavenTestFailure || mavenTestError || gradleTestFailure || gradleTestError) {
					returnValue = 0;
				} else if (returnValue != 0 && line.contains("BUILD FAILURE")) {
					returnValue = 2;
				}
			}

			String errorLine;
			while ((errorLine = error.readLine()) != null) {
				System.out.println(errorLine);
			}
			
			System.out.println("RETURN VALUE " + returnValue);
			
			if (returnValue == 0) {
				return returnValue;
			}

			if (!exitValue) {
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
			cmd = "./mvnw clean test -Dtest=" + className + "#" + testName;
		} else if (isMaven) {
			cmd = Config.MVN_DIR + " clean test -Dtest=" + className + "#" + testName;
		} else if (hasWrapper) {
			cmd = "./gradlew clean test --tests " + className + "." + testName + " -i";
		} else {
			cmd = "gradle clean test --tests " + className + "." + testName + " -i";
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
		
		if (numTests > Config.MAX_TESTS) {
			numTests = Config.MAX_TESTS;
		}
		
		List<String> order = new ArrayList<>();
		
		System.out.println("numorders " + numOrders);
		
		for (int i = 0; i < numOrders; i++) {
			order = new ArrayList<>();
			
			while(!order.contains(testName)) {
				for (int j = 0; j < numTests; j++) {
					System.out.println("adding " + testNames.get(j));
					order.add(testNames.get(j));
				}
				
				Collections.shuffle(testNames);
			}
			
			orders.add(order);
		}
		
		return orders;
	}
	
	public static TestResult runMultipleTests(Project project, String className, String testName, int numOfRuns, boolean isMaven, boolean hasWrapper) {
		multiTestName = testName;
		
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
						first = false;
					} else {
						cmd = cmd + "+" + order.get(j);
					}
				} else {
					if (first) {
						cmd = order.get(j);
						first = false;
					} else {
						cmd = cmd + " --tests " + className + "." + order.get(j);
					}
 				}
				
				System.out.println("cmd " + cmd);
			}
			
			testResult = runSingleTest(project, className, cmd, 1, isMaven, hasWrapper);
			
			if (testResult.getIfTestFailCompile()) {
				System.out.println("UIHAERIFA");
				return new TestResult(className, testName, 0, true, true);
			} else if (testResult.getFlakyness() == 1) {
				System.out.println("failure ");
				failures++;
			} 
		}
		
		flakyness = failures / numOfRuns;
		
		return new TestResult(className, testName, flakyness, true, false);
	}
}
