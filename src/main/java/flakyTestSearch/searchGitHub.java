package flakyTestSearch;

import java.util.ArrayList;
import java.util.List;

import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;

public class searchGitHub {
	/**
	 * Takes a project JSON object as input, finds the pull
	 * request URL in order to find the commit hash of that pull 
	 * request and then the hash of the parent commit to that
	 * pull request (the commit with the flakyness).
	 * 
	 * @param currentProject: a project containing a pull request fixing flakyness
	 * @return pullRequestParentHash: the hash of the commit containing flakyness
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
				.getString("sha");
		
		JSONArray pullRequestCommit
				= Unirest.get(currentProject.getString("repository_url")+"/commits")
				.basicAuth("mlai962", "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")
				.header("accept", "application/vnd.github.v3+json")
				.queryString("sha", pullRequestHash)
				.asJson()
				.getBody()
				.getArray();
		
		String pullRequestParentHash
				= pullRequestCommit // The commit API call response is a JSON array
				.getJSONObject(0) // The first JSON object in the array is the commit we entered in the query
				.getJSONArray("parents") // We are looking for the parent hash
				.getJSONObject(0) // There is only one JSON object in the parents array
				.getString("sha"); // This gets the hash of the parent of the commit we queried 
		
		return pullRequestParentHash;
	}
	
	/**
	 * Takes a keyword as input to search for in the GitHub API. 
	 * Searches through the maximum available pages of results. 
	 * Gets the project URL, hash of the commit containing the
	 * flaky test, and the test id, takes all three of these
	 * and stores them in a project object, then adds each
	 * object to a list and returns this list.
	 * 
	 * @param keywordToSearch: the keyword we are searching GitHub for
	 * @return projects: a list of projects with their URLs, hashes and test IDs
	 */
	public static List<project> APIcall(String keywordToSearch) {
		JSONObject jsonObject;
		JSONArray jsonArray;
		kong.unirest.HttpResponse<JsonNode> jsonResponse;
		List<project> projects = new ArrayList<>();
		int pageNum = 1;
		
		do {
			jsonResponse
				= Unirest.get("https://api.github.com/search/issues")
				.basicAuth("mlai962", "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")
				.header("accept", "application/vnd.github.v3+json")
				.queryString("q", keywordToSearch+" language:java")
				.queryString("page", pageNum)
				.queryString("per_page", 100)
				.asJson();
			
			jsonObject = jsonResponse.getBody().getObject();
//			System.out.println(jsonResponse.getBody().toPrettyString());
			
			if (jsonObject.length() == 3) {
				jsonArray = jsonObject.getJSONArray("items");
				
				for (int i = 0; i < jsonArray.length(); i++) {
					JSONObject currentProject = jsonArray.getJSONObject(i);
					
					String projectURL = currentProject.getString("repository_url");
					
					if (currentProject.has("pull_request")) {
						String flakyCommitHash = getCommitHashBeforePullRequest(currentProject);
						projects.add(new project(projectURL, flakyCommitHash, null));
					} else {
						projects.add(new project(projectURL, null, null));
					}
					
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
		List<project> projects = APIcall("flaky");
		
		for (int i = 0; i < projects.size(); i++) {
			System.out.println(projects.get(i).getProjectName());
			System.out.println(i);
		}
	}
}