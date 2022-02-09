package main.java.flakyTestSearch;

import java.util.List;

public class Main {
	public static void main (String args[]) {
		List<Project> projects = SearchGitHub.getProjectList("flaky");
	}
}
