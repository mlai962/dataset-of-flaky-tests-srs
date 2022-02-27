package main.java.flakyTestSearch;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
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
		
		try {
			FileWriter csvWriter = new FileWriter("flaky.csv");
			
			csvWriter.append("ProjectURL");
			csvWriter.append(",");
			csvWriter.append("CommitHash");
			csvWriter.append(",");
			csvWriter.append("ClassName.TestName");
			csvWriter.append(",");
			csvWriter.append("SkipReason");
			csvWriter.append(",");
			csvWriter.append("FailureRate");
			csvWriter.append(",");
			csvWriter.append("IsOrderDependent");
			csvWriter.append("\n");
			
			for (Project project : projects) {
				for (TestResult testResult : project.getTestResults()) {
					if (!testResult.getIfTestFailCompile()) {
						boolean isOrderDependent = testResult.getIfOrderDependent();
						String order = "false";
						if (isOrderDependent) {
							order = "true";
						}
						
						String flaky = String.valueOf(testResult.getFlakyness());
						
						List<String> list = Arrays.asList(project.getProjectURL(), 
								project.getCommitHash(), 
								testResult.getClassName() + "." + testResult.getTestName(), 
								project.getSkipReason(), 
								flaky, 
								order);
						
						csvWriter.append(String.join(",", list));
						csvWriter.append("\n");
					}
					
				}
			}
			
			csvWriter.flush();
			csvWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
