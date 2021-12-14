package flakyTestSearch;

public class project {
	private String projectName;
	private String commitHash;
	private String testID;
	
	public project(String projectName, String commitHash, String testID) {
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
