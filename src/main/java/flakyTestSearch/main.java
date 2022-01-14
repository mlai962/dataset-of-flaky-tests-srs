package flakyTestSearch;

import java.util.List;

public class Main {
	public static void main (String args[]) {
		List<Project> projects = SearchGitHub.getProjectList();
		
		for (int i = 0; i < projects.size(); i++) {
			System.out.println(projects.get(i).getProjectName());
		}
	}
}
