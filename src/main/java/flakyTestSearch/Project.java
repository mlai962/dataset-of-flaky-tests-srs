package main.java.flakyTestSearch;

import java.util.HashMap;
import java.util.List;

public class Project {
	private String projectURL;
	private String commitHash;
	private List<String> classes;
	private HashMap<String, List<String>> testNames;
	private String skipReason;
	private String projectName;
	private List<TestResult> testResults;
	
	public Project(String projectURL, String commitHash, List<String> classes, HashMap<String, List<String>> testNames) {
		this.projectURL = projectURL;
		this.commitHash = commitHash;
		this.classes = classes;
		this.testNames = testNames;
		this.skipReason = null;
	}

	public String getProjectURL() {
		return projectURL;
	}
	
	public void setProjectURL(String projectURL) {
		this.projectURL = projectURL;
	}

	public String getCommitHash() {
		return commitHash;
	}
	
	public void setCommitHash(String commitHash) {
		this.commitHash = commitHash;
	}
	
	public List<String> getClasses() {
		return classes;
	}

	public void setClasses(List<String> classes) {
		this.classes = classes;
	}
	
	public HashMap<String, List<String>> getTestNames() {
		return testNames;
	}
	
	public void setTestNames(HashMap<String, List<String>> testNames) {
		this.testNames = testNames;
	}

	public String getSkipReason() {
		return skipReason;
	}

	public void setSkipReason(String skipReason) {
		this.skipReason = skipReason;
	}
	
	public String getProjectName() {
		return projectName;
	}
	
	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public List<TestResult> getTestResults() {
		return testResults;
	}

	public void setTestResults(List<TestResult> testResults) {
		this.testResults = testResults;
	}
}