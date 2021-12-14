package flakyTestSearch;

import java.util.ArrayList;
import java.util.List;

import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;

public class searchGitHub {
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
			
			if (jsonObject.length() == 3) {
				jsonArray = jsonObject.getJSONArray("items");
				
				for (int i = 0; i < jsonArray.length(); i++) {
					JSONObject currentProject = jsonArray.getJSONObject(i);
					projects.add(new project(currentProject.getString("repository_url"), null,null));
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
//		JSONObject jsonObject = APIcall("flaky", "/search/pr");
		
		for (int i = 0; i < projects.size(); i++) {
			System.out.println(projects.get(i).getProjectName());
			System.out.println(i);
		}
	}
}