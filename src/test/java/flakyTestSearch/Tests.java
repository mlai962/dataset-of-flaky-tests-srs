package test.java.flakyTestSearch;

import main.java.flakyTestSearch.*;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.Test;

public class Tests {
	@Test
	public void testSingleGradleFailure() {
		Project project = new Project(null, null, null, null);
		project.setProjectName("gradleweb1");
		
		RepoUtil.executeCommand("./gradlew clean build -x test -x javadoc", Config.TEMP_DIR + File.separator + project.getProjectName());
		
		TestResult testResult = TestFlakyness.runSingleTest(project, "Gradleweb1ApplicationTests", "flakyTest", 10, false, true);
		
		System.out.println(testResult.getFlakyness());
		
		assertTrue(testResult.getClassName().contains("Gradleweb1ApplicationTests"));
		assertEquals("flakyTest", testResult.getTestName());
		assertTrue(testResult.getFlakyness() > 0 && testResult.getFlakyness() < 1);
		assertTrue(!testResult.getIfOrderDependent());
		assertTrue(!testResult.getIfTestFailCompile());
	}
	
	@Test
	public void testSingleGradleError() {
		Project project = new Project(null, null, null, null);
		project.setProjectName("gradleweb1");
		
		RepoUtil.executeCommand("./gradlew clean build -x test -x javadoc", Config.TEMP_DIR + File.separator + project.getProjectName());
		
		TestResult testResult = TestFlakyness.runSingleTest(project, "Gradleweb1ApplicationTests", "error", 10, false, true);
		
		System.out.println(testResult.getFlakyness());
		
		assertTrue(testResult.getClassName().contains("Gradleweb1ApplicationTests"));
		assertEquals("flakyTest", testResult.getTestName());
		assertTrue(testResult.getFlakyness() > 0 && testResult.getFlakyness() < 1);
		assertTrue(!testResult.getIfOrderDependent());
		assertTrue(!testResult.getIfTestFailCompile());
	}
	
	@Test
	public void testSingleMavenFailure() {
		Project project = new Project(null, null, null, null);
		project.setProjectName("jinjava");
		
		RepoUtil.executeCommand("./mvnw clean install -Dmaven.test.skip=true -Dmaven.javadoc.skip=true", Config.TEMP_DIR + File.separator + project.getProjectName());
		
		TestResult testResult = TestFlakyness.runSingleTest(project, "TestClass", "flakyTest", 10, true, true);
		
		System.out.println(testResult.getFlakyness());
		
		assertTrue(testResult.getClassName().contains("TestClass"));
		assertEquals("flakyTest", testResult.getTestName());
		assertTrue(testResult.getFlakyness() > 0 && testResult.getFlakyness() < 1);
		assertTrue(!testResult.getIfOrderDependent());
		assertTrue(!testResult.getIfTestFailCompile());
	}
	
	@Test
	public void testSingleMavenError() {
		Project project = new Project(null, null, null, null);
		project.setProjectName("jinjava");
		
		RepoUtil.executeCommand("./mvnw clean install -Dmaven.test.skip=true -Dmaven.javadoc.skip=true", Config.TEMP_DIR + File.separator + project.getProjectName());
		
		TestResult testResult = TestFlakyness.runSingleTest(project, "TestClass", "error", 10, true, true);
		
		System.out.println(testResult.getFlakyness());
		
		assertTrue(testResult.getClassName().contains("TestClass"));
		assertEquals("error", testResult.getTestName());
		assertTrue(testResult.getFlakyness() > 0 && testResult.getFlakyness() < 1);
		assertTrue(!testResult.getIfOrderDependent());
		assertTrue(!testResult.getIfTestFailCompile());
	}
	
	@Test
	public void testMultiGradleFailure() {
		Project project = new Project(null, null, null, null);
		project.setProjectName("gradleweb1");
		HashMap<String, List<String>> map = new HashMap<>();
		List<String> list = new ArrayList<>();
//		list.add("flakyTest");
//		list.add("error");
		list.add("flakyOrderTest2");
		list.add("flakyOrderTest1");
		map.put("Gradleweb1ApplicationTests", list);
		project.setAllTestNames("Gradleweb1ApplicationTests", list);
		
		RepoUtil.executeCommand("./gradlew clean build -x test -x javadoc", Config.TEMP_DIR + File.separator + project.getProjectName());
		
		TestResult testResult = TestFlakyness.runMultipleTests(project, "Gradleweb1ApplicationTests", "flakyOrderTest2", 10, false, true);
		
		System.out.println(testResult.getFlakyness());
		
		assertTrue(testResult.getClassName().contains("Gradleweb1ApplicationTests"));
		assertEquals("flakyOrderTest2", testResult.getTestName());
		assertTrue(testResult.getFlakyness() > 0 && testResult.getFlakyness() < 1);
		assertTrue(testResult.getIfOrderDependent());
		assertTrue(!testResult.getIfTestFailCompile());
	}
	
	@Test
	public void testMultiMavenFailure() {
		Project project = new Project(null, null, null, null);
		project.setProjectName("jinjava");
		HashMap<String, List<String>> map = new HashMap<>();
		List<String> list = new ArrayList<>();
//		list.add("flakyTest");
//		list.add("error");
		list.add("flakyOrderTest2");
		list.add("flakyOrderTest1");
		map.put("TestClass", list);
		project.setAllTestNames("TestClass", list);
		
		RepoUtil.executeCommand("./mvnw clean install -Dmaven.test.skip=true -Dmaven.javadoc.skip=true", Config.TEMP_DIR + File.separator + project.getProjectName());
		
		TestResult testResult = TestFlakyness.runMultipleTests(project, "TestClass", "flakyOrderTest1", 10, true, true);
		
		System.out.println(testResult.getFlakyness());
		
		assertTrue(testResult.getClassName().contains("jinjava"));
		assertEquals("flakyOrderTest1", testResult.getTestName());
		assertTrue(testResult.getFlakyness() > 0 && testResult.getFlakyness() < 1);
		assertTrue(testResult.getIfOrderDependent());
		assertTrue(!testResult.getIfTestFailCompile());
	}
}
