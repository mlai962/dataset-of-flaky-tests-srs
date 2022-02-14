package main.java.flakyTestSearch;

public class TestResult {
	private String className;
	private String testName;
	private double flakyness;
	private boolean ifOrderDependent;
	
	public TestResult(String className, String testName, double flakyness, boolean ifOrderDependent) {
		this.className = className;
		this.testName = testName;
		this.flakyness = flakyness;
		this.ifOrderDependent = ifOrderDependent;
	}
	
	public String getClassName() {
		return className;
	}
	
	public void setClassName(String className) {
		this.className = className;
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
