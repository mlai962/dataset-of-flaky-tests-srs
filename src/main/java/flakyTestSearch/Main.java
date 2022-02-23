package main.java.flakyTestSearch;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;

public class Main {
	public static void main (String args[]) {
		List<Project> projects = SearchGitHub.getProjectList("flaky");
		
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		
		try {
			gson.toJson(projects, new FileWriter(Config.TEMP_DIR));
		} catch (JsonIOException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
