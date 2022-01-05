package flakyTestSearch;

import java.util.ArrayList;
import java.util.List;

import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;

public class searchGitHub {
	/**
	 * 
	 */
	public static String getCommitHashBeforePullRequest(JSONObject currentProject) {
		String pullRequestURL = currentProject.getJSONObject("pull_request").getString("url");
		String pullRequestHash
				= Unirest.get(pullRequestURL)
				.basicAuth("mlai962", "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")
				.header("accept", "application/vnd.github.v3+json")
				.asJson()
				.getBody()
				.getObject()
				.getJSONObject("head")
				.getString("head");
		
		JSONObject pullRequestCommit
				= Unirest.get(currentProject.getString("repository_url"+"/commits"))
				.basicAuth("mlai962", "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")
				.header("accept", "application/vnd.github.v3+json")
				.queryString("sha", pullRequestHash)
				.asJson()
				.getBody()
				.getObject();
		
		String pullRequestParentHash = pullRequestCommit.getJSONObject("parents").getString("sha");
		
		return pullRequestParentHash;
	}
	
	/**
	 * Takes a keyword as input to search for in the GitHub api. 
	 * Also takes a URL extension as input to search for either 
	 * issues or pull requests. 
	 * @param keywordToSearch
	 */
	public static List<project> APIcall(String keywordToSearch, String apiURL) {
		JSONObject jsonObject;
		JSONArray jsonArray;
		kong.unirest.HttpResponse<JsonNode> jsonResponse;
		List<project> projects = new ArrayList<>();
		int pageNum = 1;
		
		do {
			jsonResponse
				= Unirest.get("https://api.github.com" + apiURL)
				.basicAuth("mlai962", "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")
				.header("accept", "application/vnd.github.v3+json")
				.queryString("q", keywordToSearch+" language:java")
				.queryString("page", pageNum)
				.queryString("per_page", 100)
				.asJson();
			
			jsonObject = jsonResponse.getBody().getObject();
			System.out.println(jsonResponse.getBody().toPrettyString());
			
			if (jsonObject.length() == 3) {
				jsonArray = jsonObject.getJSONArray("items");
				
				for (int i = 0; i < jsonArray.length(); i++) {
					JSONObject currentProject = jsonArray.getJSONObject(i);
					
					String projectURL = currentProject.getString("repository_url");
					String flakyCommitHash = getCommitHashBeforePullRequest(currentProject);
					
					projects.add(new project(projectURL, flakyCommitHash, null));
				}
			}
			
			pageNum++;
		} while (jsonResponse.getStatus() == 200);
		
		return projects;
	}
	
	/**
	 * 
	 */
	public static void findFlakyness() {
		List<project> projects = APIcall("flaky", "/search/issues");
		
		for (int i = 0; i < projects.size(); i++) {
			System.out.println(projects.get(i).getProjectName());
			System.out.println(i);
		}
	}
}