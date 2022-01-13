package flakyTestSearch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;

public class SearchGitHub {
	public static HttpResponse<JsonNode> apiCall (String apiURL) {
		HttpResponse<JsonNode> responseBody
				=Unirest.get(apiURL)
				.basicAuth("mlai962", "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")
				.header("accept", "application/vnd.github.v3+json")
				.asJson();
		
		return responseBody;
	}
	
	public static HttpResponse<JsonNode> apiCall (String apiURL, Map<String, Object> queryMap) {
		HttpResponse<JsonNode> responseBody
				=Unirest.get(apiURL)
				.basicAuth("mlai962", "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")
				.header("accept", "application/vnd.github.v3+json")
				.queryString(queryMap)
				.asJson();
		
		return responseBody;
	}
	
	public static JSONArray getBranchNames (JSONObject currentProject) {
		return apiCall(currentProject.getString("repository_url")+"/branches").getBody().getArray();
	}
	
	public static String getIssueCreationDate (JSONObject currentProject) {
		return currentProject.getString("created_at");
	}
	
	public static String searchIssueBranchName(JSONObject currentProject) {
		JSONArray branchNames = getBranchNames(currentProject);
		
		Boolean hasMaster = false;
		Boolean hasMain = false;
		
		for (int i = 0; i < branchNames.length(); i++) {
			Pattern pattern = Pattern.compile(branchNames.getJSONObject(i).getString("name"), Pattern.LITERAL);
			Matcher matcher = pattern.matcher(currentProject.getString("title"));
			
			if (branchNames.getJSONObject(i).getString("name").equals("master")) {
				hasMaster = true;
			}
			
			if (branchNames.getJSONObject(i).getString("name").equals("main")) {
				hasMain = true;
			}
			
			if (matcher.find()) {
				return matcher.group(0);
			}
			
			if (!currentProject.isNull("name")) {
				matcher = pattern.matcher(currentProject.getString("body"));
				
				if (matcher.find()) {
					return matcher.group(0);
				}
			}
		}
		
		if (hasMaster) {
			return "master";
		} else if (hasMain) {
			return "main";
		} else {
			return null;
		}
		
	}
	
	public static String getIssueCommitHash (JSONObject currentProject) {
		HashMap<String, Object> queryMap = new HashMap<>();
		queryMap.put("sha", searchIssueBranchName(currentProject));
		queryMap.put("until", getIssueCreationDate(currentProject));
		
		JSONArray issueCommits
				= apiCall(currentProject.getString("repository_url")+"/commits", queryMap).getBody().getArray();
		
		System.out.println(currentProject);
		System.out.println(issueCommits);
		
		if (!issueCommits.isEmpty()) {
			String issueParentHash
					= issueCommits
					.getJSONObject(0)
					.getJSONArray("parents")
					.getJSONObject(0)
					.getString("sha");
			
			return issueParentHash;
		}
		
		return null;
	}
	
	public static String getPullRequestURL (JSONObject currentProject) {
		return currentProject.getJSONObject("pull_request").getString("url");
	}
	
	/**
	 * Takes the current project and finds the pull request URL
	 * and then finds the diff related to that pull request
	 * and searches it for any name of a test which may or may
	 * not be the name of the flaky test.
	 * 
	 * @param currentProject: a project containing a pull request fixing flakyness
	 * @return pullRequestTestID: the name a test or null if none are found
	 */
	public static String getPullRequestTestID(JSONObject currentProject) {
		String pullRequestDiff
				= Unirest.get(getPullRequestURL(currentProject)) // Using the pull request URL to find its diff
				.basicAuth("mlai962", "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")
				.header("Accept", "application/vnd.github.v3.diff") // Getting the diff
				.asString()
				.getBody();
		
		// Searching the diff for a string that starts with a "/", contains "test"
		// and ends with "java"
		Pattern pattern = Pattern.compile("\\/[^\\s]*test[^\\s]*java", Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(pullRequestDiff);
		
		// If the above searching finds a match then return the test ID, otherwise
		// return nothing
		if (matcher.find()) {
			String pullRequestTestID = matcher.group(0);
			return pullRequestTestID;
		}
		
		return null;
	}
	
	/**
	 * Takes a project JSON object as input, finds the pull
	 * request URL in order to find the commit hash of that pull 
	 * request and then the hash of the parent commit to that
	 * pull request (the commit with the flakyness).
	 * 
	 * @param currentProject: a project containing a pull request fixing flakyness
	 * @return pullRequestParentHash: the hash of the commit containing flakyness
	 */
	public static String getPullRequestCommitHash(JSONObject currentProject) {
		// Using the pull request URL to find its commit hash
		String pullRequestHash
				= apiCall(getPullRequestURL(currentProject)).getBody().getObject()
				.getJSONObject("head")
				.getString("sha"); // Getting the commit hash
		
		// Set the latest commit in the API response to be the PR's commit
		HashMap<String, Object> queryMap = new HashMap<>();
		queryMap.put("sha", pullRequestHash);
		
		// Search the current repository's commits
		JSONArray pullRequestCommit
				= apiCall(currentProject.getString("repository_url")+"/commits", queryMap).getBody().getArray();
		
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
	public static List<Project> getProjects(String keywordToSearch) {
		HttpResponse<JsonNode> jsonResponse;
		JSONObject jsonObject;
		JSONArray jsonArray;
		List<Project> projects = new ArrayList<>();
		int pageNum = 1;
		
		do {
			HashMap<String, Object> queryMap = new HashMap<>();
			queryMap.put("q", keywordToSearch + " language:java");
			queryMap.put("page", pageNum);
			queryMap.put("per_page", 100);
			
			jsonResponse
				= apiCall("https://api.github.com/search/issues", queryMap);
			
//			System.out.println(jsonResponse.getBody().toPrettyString());
			
			jsonObject = jsonResponse.getBody().getObject();
			
			if (jsonObject.length() == 3) {
				jsonArray = jsonObject.getJSONArray("items");
				
				for (int i = 0; i < jsonArray.length(); i++) {
					JSONObject currentProject = jsonArray.getJSONObject(i);
					
					String projectURL = currentProject.getString("repository_url");
					
					if (currentProject.has("pull_request")) {
//						String commitHash = getPullRequestCommitHash(currentProject);
//						String testID = getPullRequestTestID(currentProject);
//						projects.add(new Project(projectURL, commitHash, testID));
					} else {
						String commitHash = getIssueCommitHash(currentProject);
						projects.add(new Project(projectURL, commitHash, null));
					}
				}
			}
			
			pageNum++;
		} while (jsonResponse.getStatus() == 200);
		
		return projects;
	}
	
	/**
	 * Searches GitHub for pull requests and issues that include
	 * keywords that are related to test flakyness.
	 */
	public static void findFlakyness() {
		List<Project> projects = getProjects("flaky");
		
		System.out.println(projects);
	}
}