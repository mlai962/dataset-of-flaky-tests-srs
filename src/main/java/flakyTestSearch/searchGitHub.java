package flakyTestSearch;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import kong.unirest.Headers;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;

public class SearchGitHub {
	public static JSONArray getBranchNames (JSONObject currentProject) {
		JSONArray branchNames
				= Unirest.get(currentProject.getString("repository_url")+"/branches")
				.basicAuth("mlai962", "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")
				.header("accept", "application/vnd.github.v3+json")
				.asJson()
				.getBody()
				.getArray();
		
		return branchNames;
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
		JSONArray issueCommits
				= Unirest.get(currentProject.getString("repository_url")+"/commits")
				.basicAuth("mlai962", "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")
				.header("accept", "application/vnd.github.v3+json")
				.queryString("sha", searchIssueBranchName(currentProject))
				.queryString("until", getIssueCreationDate(currentProject))
				.asJson()
				.getBody()
				.getArray();
		
		String issueParentHash
				= issueCommits
				.getJSONObject(0)
				.getJSONArray("parents")
				.getJSONObject(0)
				.getString("sha");
		
		return issueParentHash;
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
		String pullRequestHash
				= Unirest.get(getPullRequestURL(currentProject)) // Using the pull request URL to find its commit hash
				.basicAuth("mlai962", "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")
				.header("accept", "application/vnd.github.v3+json")
				.asJson()
				.getBody()
				.getObject()
				.getJSONObject("head")
				.getString("sha"); // Getting the commit hash
		
		JSONArray pullRequestCommit
				= Unirest.get(currentProject.getString("repository_url")+"/commits") // Search the current repository's commits
				.basicAuth("mlai962", "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")
				.header("accept", "application/vnd.github.v3+json")
				.queryString("sha", pullRequestHash) // Set the latest commit in the API response to be the PR's commit
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
	public static List<Project> APIcall(String keywordToSearch) {
		kong.unirest.HttpResponse<JsonNode> jsonResponse;
		JSONObject jsonObject;
		JSONArray jsonArray;
		List<Project> projects = new ArrayList<>();
		int pageNum = 1;
		
		do {
			jsonResponse
				= Unirest.get("https://api.github.com/search/issues")
				.basicAuth("mlai962", "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")
				.header("accept", "application/vnd.github.v3+json")
				.queryString("q", keywordToSearch + " language:java")
				.queryString("page", pageNum)
				.queryString("per_page", 100)
				.asJson();
			
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
			
			break;
		} while (jsonResponse.getStatus() == 200);
		
		return projects;
	}
	
	/**
	 * Searches GitHub for pull requests and issues that include
	 * keywords that are related to test flakyness.
	 */
	public static void findFlakyness() {
		List<Project> projects = APIcall("flaky");
	}
}