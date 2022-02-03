package flakyTestSearch;

import java.util.HashMap;
import java.util.List;

public class Project {
	private String projectName;
	private String commitHash;
	private List<String> classes;
	private HashMap<String, List<String>> testNames;
	private String skipReason;
	
	public Project(String projectName, String commitHash, List<String> classes, HashMap<String, List<String>> testNames) {
		this.projectName = projectName;
		this.commitHash = commitHash;
		this.classes = classes;
		this.testNames = testNames;
		this.skipReason = null;
	}

	public String getProjectName() {
		return projectName;
	}
	
	public void setProjectName(String projectName) {
		this.projectName = projectName;
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
}
