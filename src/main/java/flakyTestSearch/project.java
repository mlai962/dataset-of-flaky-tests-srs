package flakyTestSearch;

public class Project {
	private String projectName;
	private String commitHash;
	private String testID;
	
	public Project(String projectName, String commitHash, String testID) {
		this.projectName = projectName;
		this.commitHash = commitHash;
		this.testID = testID;
	}

	public String getProjectName() {
		return projectName;
	}

	public String getCommitHash() {
		return commitHash;
	}

	public String getTestID() {
		return testID;
	}
	
	
}
