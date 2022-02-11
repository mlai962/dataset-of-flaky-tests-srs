package main.java.flakyTestSearch;

public class TestResult {
	private String testName;
	private double flakyness;
	private boolean ifOrderDependent;
	
	public TestResult(String testName, double flakyness, boolean ifOrderDependent) {
		this.testName = testName;
		this.flakyness = flakyness;
		this.ifOrderDependent = ifOrderDependent;
	}

	public String getTestName() {
		return testName;
	}

	public void setTestName(String testName) {
		this.testName = testName;
	}

	public double getFlakyness() {
		return flakyness;
	}

	public void setFlakyness(double flakyness) {
		this.flakyness = flakyness;
	}

	public boolean isIfOrderDependent() {
		return ifOrderDependent;
	}

	public void setIfOrderDependent(boolean ifOrderDependent) {
		this.ifOrderDependent = ifOrderDependent;
	}
}
