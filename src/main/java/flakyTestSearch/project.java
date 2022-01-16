package flakyTestSearch;

public class Project {
	private String projectName;
	private String commitHash;
	private String className;
	private String testID;
	private String skipReason;
	
	public Project(String projectName, String commitHash, String className, String testID) {
		this.projectName = projectName;
		this.commitHash = commitHash;
		this.className = className;
		this.testID = testID;
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
	
	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}
	
	public String getTestID() {
		return testID;
	}
	
	public void setTestID(String testID) {
		this.testID = testID;
	}

	public String getSkipReason() {
		return skipReason;
	}

	public void setSkipReason(String skipReason) {
		this.skipReason = skipReason;
	}
}
